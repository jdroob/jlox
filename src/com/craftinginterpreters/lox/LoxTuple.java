package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;

public class LoxTuple {
    private final List<Object> list;

    LoxTuple() {
        this.list = null;
    }

    LoxTuple(List<Object> list) {
        this.list = list;
    }

    // public void append(Object value) {
    //     list.add(value);
    // }

    // public void prepend(Object value) {
    //     List<Object> newList = new ArrayList<>();
    //     newList.add(value);
    //     newList.addAll(list);
    //     list.clear();
    //     list.addAll(newList);
    // }

    // public Object popBack() {
    //     if (list.isEmpty()) return null;
    //     int idx = list.size() - 1;
    //     Object val = list.get(idx);
    //     list.remove(idx);
    //     return val;
    // }

    // public Object popFront() {
    //     if (list.isEmpty()) return null;
    //     int idx = 0;
    //     Object val = list.get(idx);
    //     list.remove(idx);
    //     return val;
    // }

    // public void clear() {
    //     list.clear();
    // }

    public Boolean isEmpty() {
        return list.isEmpty();
    }

    public Integer size() {
        return list.size();
    }

    public LoxTuple subList(int start, int end) {
        return new LoxTuple(new ArrayList<Object>().subList(start, end));
    }

    public Object getAt(int idx) {
        return list.get(idx);
    }

    // public Object set(int idx, Object rhsVal) {
    //     list.set(idx, rhsVal);
    //     return rhsVal;
    // }

    public LoxTuple add(LoxTuple rhs) {
        List<Object> newList = new ArrayList<>();
        newList.addAll(this.list);
        newList.addAll(rhs.getList());
        return new LoxTuple(newList);
    }

    public String toString() {
        if (list.isEmpty()) return "()";
        String repr = "( ";
        int i = 0;
        for (Object value : list) {
            String valueStr = value.toString();
            if (value instanceof String) {
                valueStr = "'" + valueStr + "'";
            }
            if (value instanceof Double) {
                valueStr = Interpreter.canonicalizeNum(valueStr);
            }
            if (i == list.size() - 1) {
                repr += valueStr + " )";
            } else {
                repr += valueStr + ", ";
                i += 1;
            }
        }
        return repr;
    }

    private List<Object> getList() {
        return this.list;
    }
}
