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
import org.stone.beeop.pool.exception.ObjectRecycleException;

import java.lang.reflect.Method;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

/**
 * Pooled object
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PooledObject implements Cloneable {
    final Class[] objectInterfaces;
    final RawObjectMethodFilter filter;
    final Map<ObjectMethodCacheKey, Method> methodCache;
    private final RawObjectFactory factory;

    Object key;
    Object raw;
    Class rawClass;
    volatile int state;
    ObjectBaseHandle handleInUsing;
    volatile long lastAccessTime;
    private ObjectInstancePool ownerPool;

    //***************************************************************************************************************//
    //                                  1: Pooled entry create/clone methods(2)                                      //                                                                                  //
    //***************************************************************************************************************//
    PooledObject(RawObjectFactory factory, Class[] objectInterfaces,
                 RawObjectMethodFilter filter, Map<ObjectMethodCacheKey, Method> methodCache) {
        this.factory = factory;
        this.objectInterfaces = objectInterfaces;
        this.filter = filter;
        this.methodCache = methodCache;
    }

    PooledObject setDefaultAndCopy(Object k, Object raw, int state, ObjectInstancePool pool) throws Exception {
        this.factory.setDefault(k, raw);
        PooledObject p = (PooledObject) this.clone();

        p.key = k;
        p.raw = raw;
        p.rawClass = raw.getClass();
        p.state = state;
        p.ownerPool = pool;
        p.lastAccessTime = currentTimeMillis();//first parkTime
        return p;
    }

    //***************************************************************************************************************//
    //                               2: Pooled entry business methods(4)                                             //                                                                                  //
    //***************************************************************************************************************//
    public Object getObjectKey() {
        return key;
    }

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
            this.factory.destroy(key, raw);
        }
    }

    void recycleSelf() throws Exception {
        try {
            this.handleInUsing = null;
            this.factory.reset(key, raw);
            this.ownerPool.recycle(this);
        } catch (Throwable e) {
            this.ownerPool.abandonOnReturn(this);
            if (e instanceof Exception)
                throw (Exception) e;
            else
                throw new ObjectRecycleException(e);
        }
    }
}