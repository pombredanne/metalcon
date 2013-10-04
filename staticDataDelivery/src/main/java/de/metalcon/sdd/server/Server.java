package de.metalcon.sdd.server;

import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import de.metalcon.common.Muid;
import de.metalcon.sdd.Detail;
import de.metalcon.sdd.Entity;
import de.metalcon.sdd.IdDetail;
import de.metalcon.sdd.config.Config;
import de.metalcon.sdd.error.ServerLevelDbBatchCloseSddError;
import de.metalcon.sdd.error.ServerLevelDbCloseSddError;
import de.metalcon.sdd.error.ServerLevelDbInitializationSddError;
import de.metalcon.sdd.server.queue.QueueAction;
import de.metalcon.sdd.server.queue.UpdateDependencyQueueAction;

public class Server implements ServletContextListener {

    private static enum GraphRelTypes implements RelationshipType {
        DEPENDS
    }
    
    public Config config;
    
    private DB jsonDb;
    
    private GraphDatabaseService entityGraph;
    
    Map<Muid, Node> entityGraphMuidIndex;
    
    private BlockingQueue<QueueAction> queue;

    private Worker worker;
    
    public void start() {
        config = new Config();
        
        Options options = new Options();
        options.createIfMissing(true);
        try {
            jsonDb = factory.open(new File(config.getLeveldbPath()),
                                      options);
        } catch (IOException e) {
            throw new ServerLevelDbInitializationSddError();
        }
        
        entityGraph = new GraphDatabaseFactory().newEmbeddedDatabase(
                config.getNeo4jPath());
        
        entityGraphMuidIndex = new HashMap<Muid, Node>();
        for (Node node : GlobalGraphOperations.at(entityGraph).getAllNodes())
            if (node.hasProperty("muid"))
                entityGraphMuidIndex.put(
                        new Muid((String) node.getProperty("muid")), node);
        
        queue = new PriorityBlockingQueue<QueueAction>();
        worker = new Worker(queue);
        worker.start();
    }

    public void stop() {
        worker.stop();
        worker.waitForShutdown();
        
        entityGraph.shutdown();
        
        try {
            jsonDb.close();
        } catch (IOException e) {
            throw new ServerLevelDbCloseSddError();
        }
    }
    
    public void waitUntilQueueEmpty() {
        try {
            while (!queue.isEmpty())
                Thread.sleep(100);
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            // TODO: handle this
            throw new RuntimeException();
        }
    }
    
    public boolean queueAction(QueueAction action) {
        if (action == null)
            throw new IllegalArgumentException("action was null");
        
        if (queue.contains(action))
            return true;
        else
            return queue.offer(action);
    }
    
    /**
     * @return Returns null if entity does not exist in database.
     */
    public String readEntityJson(IdDetail idDetail) {
        if (idDetail == null)
            throw new IllegalArgumentException("idDetail was null");
        
        return asString(jsonDb.get(bytes(idDetail.toString())));
    }
    
    public void updateEntityJson(Entity entity) {
        if (entity == null)
            throw new IllegalArgumentException("entity was null");
        
        WriteBatch batch = jsonDb.createWriteBatch();
        try {
            for (String metaDetail : config.details) {
                Detail   detail   = new Detail(this, metaDetail);
                IdDetail idDetail = new IdDetail(this, entity.getId(), detail);
                batch.put(bytes(idDetail.toString()),
                          bytes(entity.getJson(detail)));
            }
            
            jsonDb.write(batch);
        } finally {
            try {
                batch.close();
            } catch (IOException e) {
                throw new ServerLevelDbBatchCloseSddError();
            }
        }
    }
    
    public void deleteEntityJson(Muid id) {
        if (id == null)
            throw new IllegalArgumentException("id was null");
            
        WriteBatch batch = jsonDb.createWriteBatch();
        try {
            for (String metaDetail : config.details) {
                Detail   detail   = new Detail(this, metaDetail);
                IdDetail idDetail = new IdDetail(this, id, detail);
                batch.delete(bytes(idDetail.toString()));
            }
             jsonDb.write(batch);
        } finally {
            try {
                batch.close();
            } catch (IOException e) {
                throw new ServerLevelDbBatchCloseSddError();
            }
        }
    }
    
    /**
     * @return Returns null if entity does not exists in database.
     */
    public Map<String, String> readEntityGraph(Muid id) {
        if (id == null)
            throw new IllegalArgumentException("id was null");
        
        Node entityNode = entityGraphMuidIndex.get(id);
        if (entityNode == null)
            return null;
        
        Map<String, String> attrs = new HashMap<String, String>();
        for (String key : entityNode.getPropertyKeys())
            attrs.put(key, (String) entityNode.getProperty(key));
        return attrs;
    }

    public void updateEntityGraph(Entity entity) {
        if (entity == null)
            throw new IllegalArgumentException("entity was null");
        
        Muid id = entity.getId();
        Set<Muid> dependencies = entity.getDependencies();
        
        Transaction tx = entityGraph.beginTx();
        try {
            Node entityNode = entityGraphMuidIndex.get(id);
            if (entityNode == null) {
                entityNode = entityGraph.createNode();
                entityNode.setProperty("muid", id.toString());
                entityNode.setProperty("type", entity.getType());
                entityGraphMuidIndex.put(id, entityNode);
            } else
                for (Relationship rel : entityNode.getRelationships(
                        Direction.OUTGOING, GraphRelTypes.DEPENDS)) {
                    Node dependency = rel.getEndNode();
                    if (!dependency.hasProperty("muid"))
                        // TODO: handle this
                        throw new RuntimeException();
                    
                    Muid dependencyId =
                            new Muid((String) dependency.getProperty("muid"));
                    
                    if (!dependencies.contains(dependencyId))
                        rel.delete();
                    else
                        dependencies.remove(dependencyId);
                }
            
            for (Map.Entry<String, String> attr :
                    entity.getAttrs().entrySet()) {
                String attrName  = attr.getKey();
                String attrValue = attr.getValue();
                if (!attrName.equals("muid") && !attrName.equals("type"))
                    entityNode.setProperty(attrName, attrValue);
            }
            
            for (Muid dependencyId : dependencies) {
                Node dependency = entityGraphMuidIndex.get(dependencyId);
                if (dependency == null)
                    // TODO: handle this
                    throw new RuntimeException();
                
                entityNode.createRelationshipTo(dependency,
                                                GraphRelTypes.DEPENDS);
            }
            
            tx.success();
        } finally {
            tx.finish();
        }
    }
    
    public void deleteEntityGraph(Muid id) {
        if (id == null)
            throw new IllegalArgumentException("id was null");
        
        Transaction tx = entityGraph.beginTx();
        try {
            Node entityNode = entityGraphMuidIndex.get(id);
            if (entityNode == null) {
                // TODO: what happens if we get a delete with a invalid id: ignore or exception?
            } else {
                for (Relationship rel : entityNode.getRelationships())
                    rel.delete();
                entityNode.delete();
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }
     
    // TODO: optimize dependency update
    public void updateEntityDependencies(Muid id) {
        if (id == null)
            throw new IllegalArgumentException("id was null");
        
        Node entityNode = entityGraphMuidIndex.get(id);
        if (entityNode == null)
            // TODO: handle this
            throw new RuntimeException();
        
        for (Relationship rel : entityNode.getRelationships(
                Direction.INCOMING, GraphRelTypes.DEPENDS)) {
            Node dependencyNode = rel.getStartNode();
            if (!dependencyNode.hasProperty("muid"))
                // TODO: handle this
                throw new RuntimeException();
            
            Muid dependencyId =
                    new Muid((String) dependencyNode.getProperty("muid"));
            
            updateEntityDependencies(dependencyId);
            
            UpdateDependencyQueueAction action =
                    new UpdateDependencyQueueAction(this, dependencyId);
            if (!queueAction(action))
                // TODO: handle this
                throw new RuntimeException();
        }
    }
    
    
    public void deleteEntityDependenciesGraph(Muid id) {
        if (id == null)
            throw new IllegalArgumentException("id was null");
    }
    
//    public void writeEntity(Entity entity) {
//        Muid id = entity.getId();
////        System.out.println("writeEntity(" + id.toString() + ")");
//        
//        updateDependingOn(entity);
//        updateEntity(entity);
//        updateDependencies(muidIndex.get(id.toString()));
//    }
//
//    private void updateDependingOn(Entity entity) {
//        Muid id = entity.getId();
//        Set<Muid> dependingOn = entity.getDependingOn();
//        String idString = id.toString();
//        
//        Node entityNode = null;
//            
//        Transaction tx = graphDb.beginTx();
//        try {
//            Set<Muid> remainingDependingOn = null;
//            
//            if (!muidIndex.containsKey(idString)) {
//                entityNode = graphDb.createNode();
//                entityNode.setProperty("muid", idString);
//                entityNode.setProperty("type", entity.getType());
//                muidIndex.put(idString, entityNode);
//                
//                remainingDependingOn = dependingOn;
//            } else {
//                entityNode = muidIndex.get(idString);
//                
//                remainingDependingOn = new HashSet<Muid>();
//                remainingDependingOn.addAll(dependingOn);
//                
//                for (Relationship rel : entityNode.getRelationships(
//                        Direction.OUTGOING, GraphRelTypes.DEPENDS))
//                    if (rel.getEndNode().hasProperty("muid")) {
//                        if (!remainingDependingOn.remove((String)
//                                rel.getEndNode().getProperty("muid")))
//                            rel.delete();
//                    } else
//                        // TODO: handle this
//                        throw new RuntimeException();
//            }
//            
//            for (Muid dependId : remainingDependingOn) {
////                System.out.println("  dependingOn: " + dependId.toString());
//                if (muidIndex.containsKey(dependId.toString()))
//                    entityNode.createRelationshipTo(
//                            muidIndex.get(dependId.toString()),
//                            GraphRelTypes.DEPENDS);
//                else
//                    // TODO: handle this
//                    throw new RuntimeException();
//            }
//            
//            tx.success();
//        } finally {
//            tx.finish();
//        }
//    }
//    
//    private void updateEntity(Entity entity) {
//        WriteBatch batch = keyvalueDb.createWriteBatch();
//        try {
//            for (Detail detail : Detail.values()) {
//                if (detail == Detail.NONE)
//                    continue;
//                
//                batch.put(
//                        bytes(new IdDetail(entity.getId(), detail).toString()),
//                        bytes(entity.getJson(detail)));
//            }
//            
//            keyvalueDb.write(batch);
//        } finally {
//            try {
//                batch.close();
//            } catch (IOException e) {
//                throw new ServerLevelDbBatchCloseSddError();
//            }
//        }
//    }
//    
//    private void updateDependencies(Node entityNode) {
//        for (Relationship rel : entityNode.getRelationships(
//                Direction.INCOMING, GraphRelTypes.DEPENDS)) {
//            Node dependencyNode = rel.getStartNode();
//            Entity entity = newEntityByType(
//                    (String) dependencyNode.getProperty("type"), this);
//            entity.loadFromId(
//                    new Muid((String) dependencyNode.getProperty("muid")));
//            updateEntity(entity);
//            updateDependencies(dependencyNode);
//        }
//    }
//    
//    public void deleteEntity(Muid id) {
//        WriteBatch batch = keyvalueDb.createWriteBatch();
//        try {
//            for (Detail detail : Detail.values()) {
//                if (detail == Detail.NONE)
//                    continue;
//                
//                batch.delete(bytes(new IdDetail(id, detail).toString()));
//            }
//            
//            keyvalueDb.write(batch);
//        } finally {
//            try {
//                batch.close();
//            } catch (IOException e) {
//                throw new ServerLevelDbBatchCloseSddError();
//            }
//        }
//    }
    
    @Override
    public void contextInitialized(ServletContextEvent arg) {
        ServletContext context = arg.getServletContext();
        context.setAttribute("server", this);

        start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg) {
        stop();
    }

}
