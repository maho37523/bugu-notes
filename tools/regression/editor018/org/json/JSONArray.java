package org.json;
import com.google.gson.*;
public class JSONArray {
    final JsonArray json = new JsonArray();
    public JSONArray put(Object value) {
        json.add(value instanceof JSONObject ? ((JSONObject)value).json : new Gson().toJsonTree(value));
        return this;
    }
}
