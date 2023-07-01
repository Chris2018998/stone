/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util;

import java.util.Map;
import java.util.Objects;

/**
 * Map Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class HashMap<K, V> {
    //class HashMap<K, V> implements Map<K, V>, Cloneable, Serializable {
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int TREEIFY_THRESHOLD = 8;
    private static final int UNTREEIFY_THRESHOLD = 6;
    private static final int MIN_TREEIFY_CAPACITY = 64;
    private final float loadFactor = DEFAULT_LOAD_FACTOR;
    private transient Node<K, V>[] table;
    private transient int tableSize;
    private int threshold;

    private static class Node<K, V> implements Map.Entry<K, V> {
        private final int hash;
        private final K key;
        private V value;
        private Node<K, V> next;

        //for linkedHashMap
        private Node<K, V> prev;
        private Node<K, V> after;
        //for tree node
        private Node<K, V> left;
        private Node<K, V> right;
        private boolean isInTree;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final String toString() {
            return key + "=" + value;
        }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this) return true;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return Objects.equals(key, e.getKey())
                    && Objects.equals(value, e.getValue());
        }
    }
}
