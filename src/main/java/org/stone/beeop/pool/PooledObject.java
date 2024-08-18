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

import org.stone.beeop.BeeObjectFactory;
import org.stone.beeop.BeeObjectMethodFilter;
import org.stone.beeop.pool.exception.ObjectRecycleException;

import java.lang.reflect.Method;
import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static org.stone.beeop.pool.ObjectPoolStatics.DESC_RM_BAD;
import static org.stone.beeop.pool.ObjectPoolStatics.OBJECT_CLOSED;
import static org.stone.tools.BeanUtil.CommonLog;

/**
 * Pooled object
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PooledObject {
    final Object key;
    final Object raw;
    private final Class<?> rawType;
    private final BeeObjectFactory factory;
    private final ObjectInstancePool ownerPool;
    private final BeeObjectMethodFilter methodFilter;
    private final Map<MethodCacheKey, Method> methodMap;

    volatile int state;
    PooledObjectPlainHandle handleInUsing;
    volatile long lastAccessTime;//milliseconds

    //***************************************************************************************************************//
    //                                  1: Pooled entry create/clone methods(1)                                      //                                                                                  //
    //***************************************************************************************************************//
    PooledObject(Object key, Object raw, BeeObjectFactory factory,
                 Map<MethodCacheKey, Method> methodMap, BeeObjectMethodFilter methodFilter,
                 ObjectInstancePool ownerPool) {

        this.key = key;
        this.raw = raw;
        this.rawType = raw.getClass();
        this.factory = factory;
        this.methodMap = methodMap;
        this.methodFilter = methodFilter;
        this.ownerPool = ownerPool;

        this.lastAccessTime = currentTimeMillis();
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

    //handle call this method to get a method of object by parameter info
    Method getMethod(String name, Class<?>[] types, Object[] params) throws Exception {
        if (methodFilter != null) methodFilter.doFilter(key, name, types, params);
        MethodCacheKey key = new MethodCacheKey(name, types);
        Method method = methodMap.get(key);

        if (method == null) {
            method = rawType.getMethod(name, types);
            methodMap.put(key, method);
        }
        return method;
    }

    //handle call this method to abort this object
    void abortSelf(String reason) {
        ownerPool.abort(this, reason);
    }

    //handle call this method to recycle this object
    void recycleSelf() throws Exception {
        try {
            this.handleInUsing = null;
            this.factory.reset(key, raw);
            this.ownerPool.recycle(this);
        } catch (Throwable e) {
            this.ownerPool.abort(this, DESC_RM_BAD);
            if (e instanceof Exception)
                throw (Exception) e;
            else
                throw new ObjectRecycleException(e);
        }
    }

    //pool call this method before this object removed
    void onBeforeRemove() {
        try {
            this.state = OBJECT_CLOSED;
            this.factory.reset(key, raw);
        } catch (Throwable e) {
            if (ownerPool.isPrintRuntimeLog())
                CommonLog.warn("BeeOP({})reset object failed", ownerPool.getPoolName(), e);
        } finally {
            try {
                this.factory.destroy(key, raw);
            } catch (Throwable e) {
                if (ownerPool.isPrintRuntimeLog())
                    CommonLog.warn("BeeOP({})An error occurred when destroyed object", ownerPool.getPoolName(), e);
            }
        }
    }
}