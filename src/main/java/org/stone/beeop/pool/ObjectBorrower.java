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
 * @author Chris Liao
 * @version 1.0
 */
final class ObjectBorrower<K, V> {
    final Thread thread = Thread.currentThread();
    volatile Object state;
    PooledObject<K, V> lastUsed;

    ObjectBorrower() {
    }

    ObjectBorrower(PooledObject<K, V> lastUsed) {
        this.lastUsed = lastUsed;
    }
}