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
        int index = table.length - 1 & firstKey.hashCode();
        table[index] = new Entry(firstKey, firstValue);
        this.size = 1;
        this.threshold = (int) (table.length * DEFAULT_LOAD_FACTOR);
    }


    //***************************************************************************************************************//
    //                                           1: Private Methods                                                  //
    //***************************************************************************************************************//
    public void set(Object key, Object value) {
        if (table.length * DEFAULT_LOAD_FACTOR >= threshold) {
            expandTable(new Entry(key, value));
        } else {
            final int maxIndex = table.length - 1;
            final int index = maxIndex & key.hashCode();
            int firstFillIndex = -1;
            Entry searchedEntry = table[index];
            if (searchedEntry != null) {
                if (searchedEntry.get() == key) {
                    searchedEntry.value = value;
                    return;
                } else if (searchedEntry.get() == null) {
                    firstFillIndex = index;
                }
            }

            //search index
            int searchIndex = index + 1;
            if (searchIndex > maxIndex) searchIndex = 0;
            while (searchIndex != index) {
                Entry entry = table[searchIndex];
                if (entry.get() == key) {
                    entry.value = value;
                    return;
                }

                if (firstFillIndex == -1 && (entry == null || entry.get() == null))
                    firstFillIndex = searchIndex;
                if (++searchIndex > maxIndex) searchIndex = 0;
            }

            table[firstFillIndex] = new Entry(key, value);
        }
    }

    public Entry get(Object key) {
        int index = table.length - 1 & key.hashCode();
    }

    public void remove(Object key) {
        int index = table.length - 1 & key.hashCode();

    }

    //***************************************************************************************************************//
    //                                           2: Private Methods                                                  //
    //***************************************************************************************************************//
    private void expandTable(Entry newEntry) {
        //1: create a new Array
        int oldLen = table.length;
        int newLen = oldLen << 1;
        Entry[] newTable = new Entry[newLen];


        //2: set the new element to index(why? Priority for new when expand)
        newLen = newLen - 1;
        newTable[newLen & newEntry.get().hashCode()] = newEntry;

        //3: copy other elements to the new array
        for (int i = 0; i < oldLen; i++) {
            Entry entry = table[i];
            if (entry == null) continue;//not filled
            Object key = entry.get();
            if (key == null) continue;//gc

            int newIndex = newLen & key.hashCode();
            if (newTable[newIndex] != null)//try to search a new pos index
                newIndex = searchValidIndex(newTable, newIndex);

            newTable[newIndex] = entry;
        }

        //replace the old table
        this.table = newTable;
        this.threshold = (int) (table.length * DEFAULT_LOAD_FACTOR);
    }

    private int searchValidIndex(Entry[] table, final int curIndex) {
        final int maxIndex = table.length - 1;
        int searchIndex = curIndex + 1;
        if (searchIndex > maxIndex) searchIndex = 0;

        while (searchIndex != curIndex) {
            Entry entry = table[searchIndex];
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