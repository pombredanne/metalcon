package de.metalcon.sdd.entity;

/**
 * This file is generated by staticDataDeliveryGenerator. Changes to it are not
 * permanent. Rather change the corresponding template file in
 * "staticDataDeliverGenerator/template/".
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONValue;

import de.metalcon.common.Muid;
import de.metalcon.sdd.Detail;
import de.metalcon.sdd.IdDetail;
import de.metalcon.sdd.server.Server;

public class Musician extends Entity {

    private String name;

    private String url;

    private String active;

    private String founder;

    private String spans;

    private Band band;

    public Musician(Server server) {
        super(server);
    }

    @Override
    public String getType() {
        return "Musician";
    }

    @Override
    public void loadFromJson(String json) {
        Map<String, String> entity = parseJson(json);

        setId(new Muid(entity.get("id")));

        name = loadPrimitive(String.class, entity.get("name"));
        url = loadPrimitive(String.class, entity.get("url"));
        active = loadPrimitive(String.class, entity.get("active"));
        founder = loadPrimitive(String.class, entity.get("founder"));
        spans = loadPrimitive(String.class, entity.get("spans"));
        band = loadEntity(Band.class, entity.get("band"));
    }

    @Override
    public void loadFromCreateParams(Map<String, String[]> params) {
        setId(new Muid(getParam(params, "id")));

        name = loadPrimitive(String.class, getParam(params, "name"));
        url = loadPrimitive(String.class, getParam(params, "url"));
        active = loadPrimitive(String.class, getParam(params, "active"));
        founder = loadPrimitive(String.class, getParam(params, "founder"));
        spans = loadPrimitive(String.class, getParam(params, "spans"));
        band = loadEntity(Band.class, getParam(params, "band"));
    }

    @Override
    public void loadFromUpdateParams(Map<String, String[]> params) {
        Muid id = new Muid(getParam(params, "id"));
        IdDetail idDetail = new IdDetail(id, Detail.FULL);
        String json = server.readEntity(idDetail);
        Map<String, String> entity = parseJson(json);

        setId(new Muid(entity.get("id")));

        name = loadPrimitive(String.class, getParam(params, "name"), entity.get("name"));
        url = loadPrimitive(String.class, getParam(params, "url"), entity.get("url"));
        active = loadPrimitive(String.class, getParam(params, "active"), entity.get("active"));
        founder = loadPrimitive(String.class, getParam(params, "founder"), entity.get("founder"));
        spans = loadPrimitive(String.class, getParam(params, "spans"), entity.get("spans"));
        band = loadEntity(Band.class, getParam(params, "band"), entity.get("band"));
    }

    @Override
    public Set<Muid> getDependingOn() {
        Set<Muid> ids = new HashSet<Muid>();
        colAddIfNotNull(ids, getEntityId(band));
        return ids;
    }

    @Override
    protected void generateJson() {
        Map<String, Object> j;

        // FULL
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        j.put("name", generatePrimitive(name));
        j.put("url", generatePrimitive(url));
        j.put("active", generatePrimitive(active));
        j.put("founder", generatePrimitive(founder));
        j.put("spans", generatePrimitive(spans));
        j.put("band", idToString(getEntityId(band)));
        json.put(Detail.FULL, JSONValue.toJSONString(j));

        // SYMBOL
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        j.put("url", generatePrimitive(url));
        j.put("name", generatePrimitive(name));
        json.put(Detail.SYMBOL, JSONValue.toJSONString(j));

        // LINE
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        j.put("band", generateEntity(band, Detail.SYMBOL));
        j.put("url", generatePrimitive(url));
        j.put("name", generatePrimitive(name));
        json.put(Detail.LINE, JSONValue.toJSONString(j));

        // PARAGRAPH
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        json.put(Detail.PARAGRAPH, JSONValue.toJSONString(j));

        // PROFILE
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        json.put(Detail.PROFILE, JSONValue.toJSONString(j));

        // TOOLTIP
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        json.put(Detail.TOOLTIP, JSONValue.toJSONString(j));

        // SEARCH_ENTRY
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        json.put(Detail.SEARCH_ENTRY, JSONValue.toJSONString(j));

        // SEARCH_DETAILED
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        json.put(Detail.SEARCH_DETAILED, JSONValue.toJSONString(j));
    }

}
