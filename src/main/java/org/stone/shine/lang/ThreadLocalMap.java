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
        //@todo
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

    private static int searchTable(Entry[] table, ThreadLocal key, boolean setInd) {
        final int maxIndex = table.length - 1;
        final int hashIndex = maxIndex & key.hashCode();
        int searchIndex = -1, keyMatchedIndex = -1, firstEmptyIndex = -1;

        //loop array(search and clear gc entry)
        while (searchIndex != hashIndex) {
            if (searchIndex == -1) searchIndex = hashIndex;

            Entry entry = table[searchIndex];
            if (entry != null && entry.get() == null) //clear gc entry
                entry = table[searchIndex] = null;

            if (keyMatchedIndex == -1) {
                if (entry != null && entry.get() == key) {
                    keyMatchedIndex = searchIndex;
                } else if (setInd && firstEmptyIndex == -1 && entry == null) {
                    firstEmptyIndex = searchIndex;
                }
            }

            if (++searchIndex > maxIndex) searchIndex = 0;
        }

        if (keyMatchedIndex > -1) return keyMatchedIndex;
        return firstEmptyIndex;
    }

    private static void clearTable(Entry[] table) {
        final int maxIndex = table.length - 1;
        for (int i = 0; i < maxIndex; i++) {
            Entry entry = table[i];
            if (entry != null && entry.get() == null) table[i] = null;
        }
    }

    //***************************************************************************************************************//
    //                                           2: map Methods(3)                                                   //
    //***************************************************************************************************************//
    public Object get(ThreadLocal key) {
        int index = searchTable(table, key, false);
        if (index > -1) return table[index].value;
        return null;
    }

    public void remove(ThreadLocal key) {
        int index = searchTable(table, key, false);
        if (index > -1) table[index] = null;
    }

    public void set(ThreadLocal key, Object value) {
        int index = searchTable(table, key, true);
        Entry entry = table[index];
        if (entry != null && entry.get() == key) {//replace entry value
            entry.value = value;
        } else if (entry == null || entry.get() == null) {
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
    private static class Entry extends WeakReference<ThreadLocal> {
        private Object value;

        private Entry(ThreadLocal k, Object v) {
            super(k);
            this.value = v;
        }
    }
}