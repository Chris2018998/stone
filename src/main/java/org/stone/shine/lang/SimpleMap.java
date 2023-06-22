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
        this.threshold = (int) (table.length * DEFAULT_LOAD_FACTOR);
        int index = table.length - 1 & firstKey.hashCode();
        table[index] = new Entry(firstKey, firstValue);
    }

    public Entry get(Object key) {
        int index = table.length - 1 & key.hashCode();
        return null;
    }

    public void set(Object key, Object value) {
        int index = table.length - 1 & key.hashCode();
    }

    public void remove(Object key) {
        int index = table.length - 1 & key.hashCode();
    }

    private void expand() {

    }

    private static class Entry extends WeakReference<Object> {
        private Object value;

        private Entry(Object k, Object v) {
            super(k);
            this.value = v;
        }
    }
}