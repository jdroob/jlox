package com.craftinginterpreters.lox;

import java.util.Map;
import java.util.HashMap;

public class LoxMap {
    private final HashMap<Object, Object> map;

    LoxMap() {
        this.map = new HashMap<>();
    }

    LoxMap(Map<Object, Object> map) {
        this.map = new HashMap<>(map);
    }

    public Object put(Object key, Object value) {
        map.put(key, value);
        return value;
    }

    public void remove(Object key) {
        map.remove(key);
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public void clear() {
        map.clear();
    }

    public Boolean isEmpty() {
        return map.isEmpty();
    }

    public Integer size() {
        return map.size();
    }

    public Map.Entry<Object, Object> getAt(Integer idx) {
        if (idx < 0 || idx >= map.size()) {
            throw new RuntimeException("Map index " + idx + " out of bounds for map of size " + map.size());
        }
        
        int currentIndex = 0;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (currentIndex == idx) {
                return entry;
            }
            currentIndex++;
        }
        
        // This should never be reached due to bounds check above
        throw new RuntimeException("Unexpected error in getAt method");
    }

    public String toString() {
        if (map.isEmpty()) return "{}";
        StringBuilder repr = new StringBuilder("{ ");
        int i = 0;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            String keyStr = key.toString();
            String valueStr = value.toString();
            if (key instanceof String) {
                keyStr = "'" + keyStr + "'";
            }
            if (key instanceof Double) {
                keyStr = Interpreter.canonicalizeNum(keyStr);
            }
            if (value instanceof Double) {
                valueStr = Interpreter.canonicalizeNum(valueStr);
            }
            if (value instanceof String) {
                valueStr = "'" + valueStr + "'";
            }
            if (i == map.size() - 1) {
                repr.append(keyStr).append(": ").append(valueStr).append(" }");
            } else {
                repr.append(keyStr).append(": ").append(valueStr).append(", ");
                i += 1;
            }
        }
        return repr.toString();
    }

    private HashMap<Object, Object> getMap() {
        return this.map;
    }
}
