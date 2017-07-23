package com.example.acer.taxiapp.utils;

import java.util.ArrayList;

public class FixedSizeList<E> {

    private int maxSize;
    private ArrayList<E> list;

    public FixedSizeList(int maxSize) {
        this.maxSize = maxSize;
        this.list = new ArrayList<>();
    }

    public void insert(E element) {
        if(list.size() == maxSize) {
            list.remove(maxSize - 1);
            list.add(0, element);
        } else {
            list.add(0, element);
        }
    }

    public ArrayList<E> getElements() {
        return list;
    }

    public int size() {
        return list.size();
    }

    public void clear() {
        list.clear();
    }
}