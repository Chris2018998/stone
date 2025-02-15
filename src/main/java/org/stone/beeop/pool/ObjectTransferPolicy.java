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
 * Pooled object transfer policy interface
 *
 * @author Chris Liao
 * @version 1.0
 */
interface ObjectTransferPolicy<K, V> {

    int getStateCodeOnRelease();

    boolean tryCatch(PooledObject<K, V> p);
}