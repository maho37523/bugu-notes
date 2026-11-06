package org.json;
import com.google.gson.*;
import java.util.*;
public class JSONObject {
    public static final Object NULL = new Object();
    final JsonObject json;
    public JSONObject() { json = new JsonObject(); }
    public JSONObject(String s) { json = JsonParser.parseString(s).getAsJsonObject(); }
    JSONObject(JsonObject o) { json=o; }
    static JsonElement encode(Object value) {
        if(value==null || value==NULL) return JsonNull.INSTANCE;
        if(value instanceof JSONObject) return ((JSONObject)value).json;
        if(value instanceof JSONArray) return ((JSONArray)value).json;
        return new Gson().toJsonTree(value);
    }
    static Object decode(JsonElement value) {
        if(value==null) return null;
        if(value.isJsonNull()) return NULL;
        if(value.isJsonObject()) return new JSONObject(value.getAsJsonObject());
        if(value.isJsonArray()) return new JSONArray(value.getAsJsonArray());
        JsonPrimitive p=value.getAsJsonPrimitive();
        if(p.isBoolean()) return p.getAsBoolean();
        if(p.isNumber()) { String n=p.getAsString();return n.contains(".") ? (Object)p.getAsDouble() : p.getAsLong(); }
        return p.getAsString();
    }
    public JSONObject put(String key,Object value) { json.add(key,encode(value));return this; }
    public JSONObject put(String key,boolean value) { return put(key,(Object)value); }
    public JSONObject put(String key,int value) { return put(key,(Object)value); }
    public JSONObject put(String key,long value) { return put(key,(Object)value); }
    public Object get(String key) { if(!json.has(key)) throw new IllegalArgumentException(key); return decode(json.get(key)); }
    public Object opt(String key) { return decode(json.get(key)); }
    public boolean isNull(String key) { return !json.has(key) || json.get(key).isJsonNull(); }
    public boolean has(String key) { return json.has(key); }
    public JSONArray optJSONArray(String key) { try { return new JSONArray(json.getAsJsonArray(key)); } catch(Exception e) { return null; } }
    public JSONObject optJSONObject(String key) { try { return new JSONObject(json.getAsJsonObject(key)); } catch(Exception e) { return null; } }
    public Object remove(String key) { return decode(json.remove(key)); }
    public Iterator<String> keys() { return json.keySet().iterator(); }
    public String optString(String key) { return optString(key,""); }
    public String optString(String key,String fallback) { Object v=opt(key);return v==null ? fallback : v==NULL ? "null" : v.toString(); }
    public int optInt(String key) { return optInt(key,0); }
    public int optInt(String key,int fallback) { try{return json.get(key).getAsInt();}catch(Exception e){return fallback;} }
    public long optLong(String key) { try{return json.get(key).getAsLong();}catch(Exception e){return 0L;} }
    public boolean optBoolean(String key) { try{return json.get(key).getAsBoolean();}catch(Exception e){return false;} }
    public String getString(String key) { return json.get(key).getAsString(); }
    public JSONArray getJSONArray(String key) { return new JSONArray(json.getAsJsonArray(key)); }
    public JSONObject getJSONObject(String key) { return new JSONObject(json.getAsJsonObject(key)); }
    public String toString() { return json.toString(); }
}
