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
import org.stone.beeop.exception.ObjectRecycledException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.stone.beeop.pool.ObjectPoolStatics.DESC_RM_BAD;
import static org.stone.beeop.pool.ObjectPoolStatics.OBJECT_CLOSED;

/**
 * Pooled object
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PooledObject<K, V> {
    final K key;
    private final BeeObjectFactory<K, V> factory;
    private final ObjectKeyCategoryPool<K, V> ownerPool;
    private final Map<MethodCacheKey, Method> methodCacheMap;

    V raw;
    volatile int state;
    volatile ObjectCreatingInfo creatingInfo;
    PooledObjectPlainHandle<K, V> handleInUsing;
    volatile long lastAccessTime;//nanoSeconds;
    private Class<V> rawType;

    //***************************************************************************************************************//
    //                                  1: constructor                                                               //                                                                                  //
    //***************************************************************************************************************//
    PooledObject(K key, BeeObjectFactory<K, V> factory,
                 Map<MethodCacheKey, Method> methodCacheMap,
                 ObjectKeyCategoryPool<K, V> ownerPool) {

        this.key = key;
        this.factory = factory;
        this.methodCacheMap = methodCacheMap;
        this.ownerPool = ownerPool;
    }

    //***************************************************************************************************************//
    //                                  2: set raw object                                                            //                                                                                  //
    //***************************************************************************************************************//
    void setRawObject(int state, V raw) {
        this.raw = raw;
        this.rawType = (Class<V>) raw.getClass();
        this.state = state;
        this.lastAccessTime = System.currentTimeMillis();
    }

    //***************************************************************************************************************//
    //                               3: Pooled entry business methods(3)                                             //                                                                                  //
    //***************************************************************************************************************//
    public String toString() {
        return this.raw.toString();
    }

    void updateAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    //***************************************************************************************************************//
    //                               4: Pooled entry business methods(4)                                             //                                                                                  //
    //***************************************************************************************************************//
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
                throw new ObjectRecycledException(e);
        }
    }

    //pool call this method before this object removed
    void onRemove(String cause) {
        ownerPool.logPrinter.info("BeeOP({}))begin to remove a pooled object:{} for cause:{}", ownerPool.getPoolName(), this, cause);

        try {
            this.factory.reset(key, raw);
        } catch (Throwable e) {
            ownerPool.logPrinter.warn("BeeOP({})reset object failed", ownerPool.getPoolName(), e);
        } finally {
            try {
                this.factory.destroy(key, raw);
            } catch (Throwable e) {
                ownerPool.logPrinter.warn("BeeOP({})An error occurred when destroyed object", ownerPool.getPoolName(), e);
            }

            this.state = OBJECT_CLOSED;
        }
    }

    //handle call this method to get a method of object by parameter info
    Method getMethod(String name, Class<?>[] types, Object[] params) throws Exception {
        MethodCacheKey key = new MethodCacheKey(name, types);
        Method method = methodCacheMap.get(key);

        if (method == null) {
            method = rawType.getMethod(name, types);
            methodCacheMap.put(key, method);
        }
        return method;
    }
}