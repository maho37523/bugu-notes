package android.util;
import java.util.LinkedHashMap;
public class LruCache<K,V> {
    private final LinkedHashMap<K,V> map = new LinkedHashMap<>();
    private final int max;
    public LruCache(int max) { this.max = max; }
    public V get(K k) { return map.get(k); }
    public V put(K k, V v) {
        V result = map.put(k,v);
        if (map.size() > max) map.remove(map.keySet().iterator().next());
        return result;
    }
}
