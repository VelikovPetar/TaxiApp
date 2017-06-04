package com.example.acer.taxiapp;

import java.util.ArrayList;

public class FixedSizeList<E> {

    private int maxSize;
    private int currentSize;
    private ArrayList<E> list;

    public FixedSizeList(int maxSize) {
        this.maxSize = maxSize;
        this.currentSize = 0;
        this.list = new ArrayList<>();
    }

    public void insert(E element) {
        if(currentSize == maxSize) {
            list.remove(maxSize - 1);
            list.add(0, element);
        } else {
            list.add(0, element);
            currentSize ++;
        }
    }

    public ArrayList<E> getElements() {
        return list;
    }

    public int size() {
        return list.size();
    }
}