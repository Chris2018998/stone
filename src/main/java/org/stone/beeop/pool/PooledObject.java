/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop.pool;

import org.stone.beeop.RawObjectFactory;
import org.stone.beeop.RawObjectMethodFilter;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.currentTimeMillis;

/**
 * Pooled Entry
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PooledObject<E> implements Cloneable {
    final Class[] objectInterfaces;
    final RawObjectMethodFilter filter;
    final ConcurrentHashMap<ObjectMethodKey, Method> methodCache;
    private final ObjectPool pool;
    private final RawObjectFactory<E> factory;

    E raw;
    Class rawClass;
    volatile int state;
    ObjectBaseHandle handleInUsing;
    volatile long lastAccessTime;

    //***************************************************************************************************************//
    //                                  1: Pooled entry create/clone methods(2)                                      //                                                                                  //
    //***************************************************************************************************************//
    PooledObject(ObjectPool pool, RawObjectFactory<E> factory, Class[] objectInterfaces,
                 RawObjectMethodFilter filter, ConcurrentHashMap<ObjectMethodKey, Method> methodCache) {

        this.pool = pool;
        this.factory = factory;
        this.objectInterfaces = objectInterfaces;
        this.methodCache = methodCache;
        this.filter = filter;
    }

    PooledObject<E> setDefaultAndCopy(E raw, int state) throws Exception {
        this.factory.setDefault(raw);
        PooledObject<E> p = (PooledObject<E>) this.clone();

        p.raw = raw;
        p.rawClass = raw.getClass();
        p.state = state;
        p.lastAccessTime = currentTimeMillis();//first parkTime
        return p;
    }

    //***************************************************************************************************************//
    //                               2: Pooled entry business methods(4)                                             //                                                                                  //
    //***************************************************************************************************************//
    public String toString() {
        return this.raw.toString();
    }

    void updateAccessTime() {
        this.lastAccessTime = currentTimeMillis();
    }

    //called by pool before remove from pool
    void onBeforeRemove() {
        try {
            state = ObjectPoolStatics.OBJECT_CLOSED;
        } catch (Throwable e) {
            ObjectPoolStatics.CommonLog.error("Object close error", e);
        } finally {
            this.factory.destroy(this.raw);
        }
    }

    void recycleSelf() throws Exception {
        try {
            this.handleInUsing = null;
            this.factory.reset(this.raw);
            this.pool.recycle(this);
        } catch (Throwable e) {
            this.pool.abandonOnReturn(this);
            if (e instanceof Exception)
                throw (Exception) e;
            else
                throw new Exception(e);
        }
    }
}