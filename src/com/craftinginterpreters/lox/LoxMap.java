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

    public void put(Object key, Object value) {
        map.put(key, value);
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
