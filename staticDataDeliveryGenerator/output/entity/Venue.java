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

public class Venue extends Entity {

    public Venue(Server server) {
        super(server);
    }

    @Override
    public String getType() {
        return "Venue";
    }

    @Override
    public void loadFromJson(String json) {
        Map<String, String> entity = parseJson(json);

        setId(new Muid(entity.get("id")));

    }

    @Override
    public void loadFromCreateParams(Map<String, String[]> params) {
        setId(new Muid(getParam(params, "id")));

    }

    @Override
    public void loadFromUpdateParams(Map<String, String[]> params) {
        Muid id = new Muid(getParam(params, "id"));
        IdDetail idDetail = new IdDetail(id, Detail.FULL);
        String json = server.readEntity(idDetail);
        Map<String, String> entity = parseJson(json);

        setId(new Muid(entity.get("id")));

    }

    @Override
    public Set<Muid> getDependingOn() {
        Set<Muid> ids = new HashSet<Muid>();
        return ids;
    }

    @Override
    protected void generateJson() {
        Map<String, Object> j;

        // FULL
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        json.put(Detail.FULL, JSONValue.toJSONString(j));

        // SYMBOL
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
        json.put(Detail.SYMBOL, JSONValue.toJSONString(j));

        // LINE
        j = new HashMap<String, Object>();
        j.put("id", getId().toString());
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
