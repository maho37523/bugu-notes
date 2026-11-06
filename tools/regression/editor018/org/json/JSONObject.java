package org.json;
import com.google.gson.*;
public class JSONObject {
    final JsonObject json;
    public JSONObject() { json = new JsonObject(); }
    public JSONObject(String s) { json = JsonParser.parseString(s).getAsJsonObject(); }
    public JSONObject put(String key, Object value) {
        JsonElement element = value instanceof JSONObject ? ((JSONObject)value).json :
            value instanceof JSONArray ? ((JSONArray)value).json : new Gson().toJsonTree(value);
        json.add(key, element); return this;
    }
    public JSONObject put(String key, boolean value) { return put(key, (Object) value); }
    public String toString() { return json.toString(); }
}
