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

import org.stone.beeop.BeeObjectPredicate;

import java.lang.reflect.Constructor;

/**
 * Object proxy
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public final class PooledObjectProxyHandle<K, V> extends PooledObjectPlainHandle<K, V> {
    private final V objectProxy;

    PooledObjectProxyHandle(PooledObject<K, V> p, BeeObjectPredicate predicate, Constructor<?> proxyClassConstructor) throws Exception {
        super(p, predicate);
        this.objectProxy = (V) proxyClassConstructor.newInstance(p, this, predicate);
    }

    @Override
    public V getObjectProxy() throws Exception {
        this.checkClosed();
        return objectProxy;
    }
}
