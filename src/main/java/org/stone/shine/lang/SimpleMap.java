/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.lang;

import java.lang.ref.WeakReference;

/**
 * A Simple Map
 *
 * @author Chris Liao
 * @version 1.0
 */

class SimpleMap {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private int size = 0;
    private Entry[] table;
    private int threshold;

    SimpleMap(Object firstKey, Object firstValue) {
        this.table = new Entry[16];
        int index = getTableIndex(firstKey, table.length - 1);
        table[index] = new Entry(firstKey, firstValue);
        this.size = 1;
        this.threshold = (int) (table.length * DEFAULT_LOAD_FACTOR);
    }


    //***************************************************************************************************************//
    //                                           1: Private Methods                                                  //
    //***************************************************************************************************************//
    public Entry get(Object key) {
        int index = getTableIndex(key, table.length - 1);
        return null;
    }

    public void set(Object key, Object value) {
        int index = getTableIndex(key, table.length - 1);
    }

    public void remove(Object key) {
        int index = getTableIndex(key, table.length - 1);
    }


    //***************************************************************************************************************//
    //                                           2: Private Methods                                                  //
    //***************************************************************************************************************//
    private int getTableIndex(Object key, int tableLen) {
        return tableLen & key.hashCode();
    }

    private void expandTable(Entry newEntry) {
        //1: create a new Array
        int oldLen = table.length;
        int newLen = oldLen << 1;
        Entry[] newTable = new Entry[newLen];

        //2: set the new element to index(why? Priority for new when expand)
        newLen = newLen - 1;
        newTable[getTableIndex(newEntry.get(), newLen)] = newEntry;

        //3: copy other elements to the new array
        for (int i = 0; i < oldLen; i++) {
            Entry entry = table[i];
            if (entry == null) continue;//not filled
            Object key = entry.get();
            if (key == null) continue;//gc

            int newIndex = getTableIndex(key, newLen);
            if (newTable[newIndex] != null)//try to search a new pos index
                newIndex = searchValidIndex(newTable, newIndex);

            newTable[newIndex] = entry;
        }

        //replace the old table
        this.table = newTable;
    }

    private int searchValidIndex(Entry[] newTable, final int curIndex) {
        final int maxIndex = newTable.length - 1;
        int searchIndex = curIndex + 1;
        if (searchIndex > maxIndex) searchIndex = 0;

        while (searchIndex != curIndex) {
            Entry entry = newTable[searchIndex];
            if (entry == null || entry.get() == null) break;
            if (++searchIndex > maxIndex) searchIndex = 0;
        }
        return searchIndex;
    }

    //***************************************************************************************************************//
    //                                          3: Map Entry                                                         //
    //***************************************************************************************************************//
    private static class Entry extends WeakReference<Object> {
        private Object value;

        private Entry(Object k, Object v) {
            super(k);
            this.value = v;
        }
    }
}