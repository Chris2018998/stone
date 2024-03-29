/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.lang;

import java.lang.ref.WeakReference;

/**
 * Thread Local Map Impl
 *
 * @author Chris Liao
 * @version 1.0
 */

class ThreadLocalMap {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private int size = 0;
    private Entry[] table;
    private int threshold;

    ThreadLocalMap(ThreadLocal firstKey, Object firstValue) {
        this.table = new Entry[16];
        int index = table.length - 1 & firstKey.hashCode();
        table[index] = new Entry(firstKey, firstValue);
        this.size = 1;
        this.threshold = (int) (table.length * DEFAULT_LOAD_FACTOR);
    }

    //support InheritThreadLocal
    ThreadLocalMap(ThreadLocalMap parent) {
        Entry[] parentTable = parent.table;
        this.table = new Entry[parentTable.length];
        for (Entry entry : parentTable) {
            if (entry == null) continue;
            ThreadLocal key = entry.get();
            if (key == null) continue;

            int index = searchTable(table, key, true);
            this.table[index] = entry;
            this.size++;
        }

        this.threshold = (int) (table.length * DEFAULT_LOAD_FACTOR);
    }

    //***************************************************************************************************************//
    //                                           1: private static Methods                                           //
    //***************************************************************************************************************//
    private static Entry[] expandTable(Entry[] oldTable) {
        //1: create a new Array
        int oldLen = oldTable.length;
        int newLen = oldLen << 1;
        Entry[] newTable = new Entry[newLen];
        newLen--;

        //2: copy entry to new array
        for (int i = 0; i < oldLen; i++) {
            Entry entry = oldTable[i];
            if (entry == null) continue;//need't filled
            ThreadLocal key = entry.get();
            if (key == null) continue;

            int index = newLen & key.hashCode();
            if (newTable[index] == null) {
                newTable[index] = new Entry(key, entry.value);
            } else {
                index = searchTable(newTable, key, true);
                newTable[index] = entry;
            }
        }
        return newTable;
    }

    private static int searchTable(Entry[] table, ThreadLocal<?> key, boolean setInd) {
        final int maxIndex = table.length - 1;
        final int hashIndex = maxIndex & key.hashCode();

        //loop array(search and clear gc entry)
        int searchIndex = hashIndex, keyMatchedIndex = -1, firstEmptyIndex = -1;
        do {
            Entry entry = table[searchIndex];
            if (entry != null && entry.get() == null) //clear gc entry
                entry = table[searchIndex] = null;

            if (keyMatchedIndex == -1) {
                if (entry != null && entry.get() == key) {
                    keyMatchedIndex = searchIndex;
                } else if (firstEmptyIndex == -1 && entry == null && setInd) {
                    firstEmptyIndex = searchIndex;
                }
            }

            if (++searchIndex > maxIndex) searchIndex = 0;
        } while (searchIndex != hashIndex);

        return keyMatchedIndex > -1 ? keyMatchedIndex : firstEmptyIndex;
    }

    //***************************************************************************************************************//
    //                                           2: map Methods(3)                                                   //
    //***************************************************************************************************************//
    public Object get(ThreadLocal<?> key) {
        int index = searchTable(table, key, false);
        if (index > -1) return table[index].value;
        return null;
    }

    public void remove(ThreadLocal<?> key) {
        int index = searchTable(table, key, false);
        if (index > -1) table[index] = null;
    }

    public void set(ThreadLocal<?> key, Object value) {
        int index = searchTable(table, key, true);
        Entry entry = table[index];
        if (entry != null) {//replace entry value
            entry.value = value;
        } else {
            table[index] = new Entry(key, value);
            if (++size >= threshold) {//expand table
                this.table = expandTable(table);
                this.threshold = (int) (table.length * DEFAULT_LOAD_FACTOR);
            }
        }
    }

    //***************************************************************************************************************//
    //                                          3: Map Entry                                                         //
    //***************************************************************************************************************//
    private static class Entry extends WeakReference<ThreadLocal<?>> {
        private Object value;

        private Entry(ThreadLocal<?> k, Object v) {
            super(k);
            this.value = v;
        }
    }
}