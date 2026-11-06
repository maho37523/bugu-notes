package org.json;
import com.google.gson.*;
import java.util.*;
public class JSONArray {
    final JsonArray json;
    public JSONArray() { json=new JsonArray(); }
    public JSONArray(String s) { json=JsonParser.parseString(s).getAsJsonArray(); }
    public JSONArray(Collection<?> c) { json=new JsonArray();for(Object o:c)put(o); }
    JSONArray(JsonArray a) { json=a; }
    public JSONArray put(Object o) { json.add(JSONObject.encode(o));return this; }
    public int length() { return json.size(); }
    public JSONObject optJSONObject(int i) { try { return new JSONObject(json.get(i).getAsJsonObject()); } catch(Exception e) { return null; } }
    public String optString(int i) { try { return json.get(i).getAsString(); } catch(Exception e) { return ""; } }
    public JSONObject getJSONObject(int i) { return new JSONObject(json.get(i).getAsJsonObject()); }
    public String getString(int i) { return json.get(i).getAsString(); }
    public String toString() { return json.toString(); }
}
