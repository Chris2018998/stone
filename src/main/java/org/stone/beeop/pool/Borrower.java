/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

/**
 * Pooled object borrower
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
final class Borrower<K, V> {
    final Thread thread;
    volatile Object state;
    PooledObject<K, V> lastUsed;

    Borrower(Thread thread) {
        this.thread = thread;
    }

    Borrower(Thread thread, PooledObject<K, V> lastUsed) {
        this.thread = thread;
        this.lastUsed = lastUsed;
    }
}