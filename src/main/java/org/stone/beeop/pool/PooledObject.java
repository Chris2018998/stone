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

import org.stone.beeop.BeeMethodLog;
import org.stone.beeop.BeeObjectFactory;
import org.stone.beeop.BeeObjectPredicate;
import org.stone.beeop.exception.BeePooledObjectRecycleException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static org.stone.beeop.BeeMethodLog.Type_Object_Log;
import static org.stone.beeop.pool.ObjectPoolStatics.DESC_RM_BAD;
import static org.stone.beeop.pool.ObjectPoolStatics.OBJECT_CLOSED;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * Pooled object
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class PooledObject<K, V> {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    final K key;
    private final PooledObjectBucket<K, V> bucket;
    private final boolean hasConfiguredMethodNames;
    private final String[] configuredMethodNames;
    private final BeeObjectPredicate objectPredicate;
    private final BeeObjectFactory<K, V> objectFactory;
    private final Map<MethodKey, MethodHandle> objectMethodCacheMap;

    //object instance
    V objectInstance;
    //state of pooled object
    volatile int state;
    //last accessed time
    volatile long lastAccessTime;
    //creation info
    volatile ObjectCreatingInfo creatingInfo;
    //handle in using
    ObjectHandleImpl<K, V> handleInUsing;
    //class type of object
    private Class<V> objectType;

    //***************************************************************************************************************//
    //                                  1: constructor(1+0)                                                           //                                                                                  //
    //***************************************************************************************************************//
    PooledObject(K key,
                 PooledObjectBucket<K, V> ownerPool,
                 BeeObjectFactory<K, V> objectFactory,
                 BeeObjectPredicate objectPredicate,
                 boolean hasConfiguredMethodNames,
                 String[] configuredMethodNames,
                 Map<MethodKey, MethodHandle> objectMethodCacheMap) {

        this.key = key;
        this.bucket = ownerPool;
        this.objectFactory = objectFactory;
        this.objectPredicate = objectPredicate;
        this.objectMethodCacheMap = objectMethodCacheMap;
        this.hasConfiguredMethodNames = hasConfiguredMethodNames;
        this.configuredMethodNames = configuredMethodNames;
    }

    //***************************************************************************************************************//
    //                                  2: set created object instance(1+0)                                          //                                                                                  //
    //***************************************************************************************************************//
    void setObjectInstance(int state, V objectInstance) {
        this.objectInstance = objectInstance;
        this.objectType = (Class<V>) objectInstance.getClass();
        this.state = state;
        this.lastAccessTime = System.currentTimeMillis();
    }

    //***************************************************************************************************************//
    //                                  3: access time update(3+0)                                                   //                                                                                  //
    //***************************************************************************************************************//
    public String toString() {
        return this.objectInstance.toString();
    }

    long updateAccessTime() {
        return this.lastAccessTime = System.currentTimeMillis();
    }

    //***************************************************************************************************************//
    //                                  4: Object recycle and destroy(0+3)                                            //                                                                                  //
    //***************************************************************************************************************//
    //pool close related pooled object and remove it from pool when handle method 'abort' is called
    void abortSelf(String reason) {
        bucket.abort(this, reason);
    }

    //pool recycle pooled object to be reused for other borrowers
    void recycleSelf() throws Exception {
        try {
            this.handleInUsing = null;
            this.objectFactory.reset(key, objectInstance);//reset dirty properties
            this.bucket.recycle(this);//assign it to one of waiters in pool
        } catch (Throwable e) {
            this.bucket.abort(this, DESC_RM_BAD);//remove it by force when exception occurred during recycle
            if (e instanceof Exception)
                throw (Exception) e;
            else
                throw new BeePooledObjectRecycleException(e);
        }
    }

    //Clear pooled object before it is removed from pool
    void onRemove(String cause) {
        bucket.logPrinter.info("BeeOP({})-begin to remove a pooled object:{} for cause:{}", bucket.getKeyName(), this, cause);

        try {
            this.objectFactory.reset(key, objectInstance);
        } catch (Throwable e) {
            bucket.logPrinter.warn("BeeOP({})-reset object failed", bucket.getKeyName(), e);
        } finally {
            try {
                this.objectFactory.destroy(key, objectInstance);
            } catch (Throwable e) {
                bucket.logPrinter.warn("BeeOP({})-an error occurred when destroyed object", bucket.getKeyName(), e);
            }

            this.state = OBJECT_CLOSED;
        }
    }

    //***************************************************************************************************************//
    //                                  5: Object method invocation(0+3)                                             //                                                                                  //
    //***************************************************************************************************************//
    Object callMethod(String name, Class<?>[] types, Object[] params) throws Throwable {
        if (isBlank(name)) throw new IllegalArgumentException("Method name can't be null or be blank");
        //if (types == null) throw new IllegalArgumentException("Method parameter types can't be null or empty");

        if (!this.hasConfiguredMethodNames || isInConfiguredMethodNames(name)) {
            BeeMethodLog<K> log = null;
            if (bucket.collectMethodLogs)
                log = bucket.beforeCall(System.currentTimeMillis(), key, Type_Object_Log, name, params);

            long updateTime = 0L;
            Object callResult = null;
            try {
                callResult = callInternal(name, types, params);
                updateTime = this.updateAccessTime();
                return callResult;
            } catch (Throwable e) {
                callResult = e;
                if (objectPredicate != null && isNotBlank(objectPredicate.evictionTest(e)))
                    handleInUsing.abort();
                throw e;
            } finally {
                if (log != null) {
                    if (updateTime == 0L) updateTime = System.currentTimeMillis();
                    bucket.afterCall(updateTime, callResult, log);//log of exception
                }
            }
        } else {
            return callInternal(name, types, params);//method name not in configuredMethodNames
        }
    }

    private boolean isInConfiguredMethodNames(String callMethodName) {
        for (String configuredName : configuredMethodNames) {
            if (callMethodName.equals(configuredName)) return true;
        }
        return false;
    }

    private Object callInternal(String name, Class<?>[] types, Object[] params) throws Throwable {
        MethodKey key = new MethodKey(name, types);
        MethodHandle methodHandle = objectMethodCacheMap.get(key);
        if (methodHandle == null) {
            methodHandle = lookup.unreflect(objectType.getMethod(name, types));
            objectMethodCacheMap.putIfAbsent(key, methodHandle);
        }

        int parameterLen = types != null ? types.length : 0;
        Object[] invokeParameters = new Object[parameterLen + 1];
        invokeParameters[0] = objectInstance;
        if (params != null && params.length > 0) {
            int copyLen = Math.min(parameterLen, params.length);
            System.arraycopy(params, 0, invokeParameters, 1, copyLen);
        }
        return methodHandle.invokeWithArguments(invokeParameters);
    }
}