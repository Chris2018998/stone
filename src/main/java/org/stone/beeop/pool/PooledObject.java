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
import org.stone.beeop.exception.BeePooledObjectRecycledException;

import java.lang.reflect.Method;
import java.util.List;
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
    //pooled key
    final K key;
    //method names to support accessed time update,eviction test,logs collection
    final List<String> objectMethodNameList;
    //Category pool,which collects method logs of object
    final ObjectKeyCategoryPool<K, V> pool;

    //destroy objects,reset objects
    private final BeeObjectFactory<K, V> objectFactory;
    //sharable map to store method of object type
    private final Map<MethodKey, Method> objectMethodCacheMap;

    //object
    V raw;
    //state of pooled object
    volatile int state;
    //last accessed time
    volatile long lastAccessTime;
    //creation info
    volatile ObjectCreatingInfo creatingInfo;
    //handle in using
    ObjectHandleImpl<K, V> handleInUsing;

    //class type of object
    private Class<V> rawType;

    //***************************************************************************************************************//
    //                                  1: constructor                                                               //                                                                                  //
    //***************************************************************************************************************//
    PooledObject(K key,
                 ObjectKeyCategoryPool<K, V> pool,
                 BeeObjectFactory<K, V> objectFactory,
                 List<String> objectMethodNameList,
                 Map<MethodKey, Method> objectMethodCacheMap) {

        this.key = key;
        this.pool = pool;
        this.objectFactory = objectFactory;
        this.objectMethodNameList = objectMethodNameList;
        this.objectMethodCacheMap = objectMethodCacheMap;
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

    void updateAccessTime(long time) {
        this.lastAccessTime = time;
    }

    //***************************************************************************************************************//
    //                               4: Pooled entry business methods(4)                                             //                                                                                  //
    //***************************************************************************************************************//
    //handle call this method to abort this object
    void abortSelf(String reason) {
        pool.abort(this, reason);
    }

    //handle call this method to recycle this object
    void recycleSelf() throws Exception {
        try {
            this.handleInUsing = null;
            this.objectFactory.reset(key, raw);
            this.pool.recycle(this);
        } catch (Throwable e) {
            this.pool.abort(this, DESC_RM_BAD);
            if (e instanceof Exception)
                throw (Exception) e;
            else
                throw new BeePooledObjectRecycledException(e);
        }
    }

    //pool call this method before this object removed
    void onRemove(String cause) {
        pool.logPrinter.info("BeeOP({})-begin to remove a pooled object:{} for cause:{}", pool.getPoolName(), this, cause);

        try {
            this.objectFactory.reset(key, raw);
        } catch (Throwable e) {
            pool.logPrinter.warn("BeeOP({})-reset object failed", pool.getPoolName(), e);
        } finally {
            try {
                this.objectFactory.destroy(key, raw);
            } catch (Throwable e) {
                pool.logPrinter.warn("BeeOP({})-an error occurred when destroyed object", pool.getPoolName(), e);
            }

            this.state = OBJECT_CLOSED;
        }
    }

    //handle call this method to get a method of object by parameter info
    Method getMethod(String name, Class<?>[] types, Object[] params) throws Exception {
        MethodKey key = new MethodKey(name, types);
        Method method = objectMethodCacheMap.get(key);

        if (method == null) {
            method = rawType.getMethod(name, types);
            objectMethodCacheMap.put(key, method);
        }
        return method;
    }
}