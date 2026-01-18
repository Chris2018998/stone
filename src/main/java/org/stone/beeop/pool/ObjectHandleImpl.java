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

import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPredicate;
import org.stone.beeop.exception.BeePooledObjectCalledException;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * object Handle implement
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public class ObjectHandleImpl<K, V> implements BeeObjectHandle<K, V> {
    protected final Object raw;
    protected final PooledObject<K, V> p;
    protected final BeeObjectPredicate predicate;
    protected final List<String> objectMethodNameList;
    private boolean isClosed;

    ObjectHandleImpl(PooledObject<K, V> p, BeeObjectPredicate predicate) {
        this.p = p;
        this.raw = p.raw;
        p.handleInUsing = this;
        this.predicate = predicate;
        this.objectMethodNameList = p.objectMethodNameList;
    }

    //***************************************************************************************************************//
    //                                     1: Handle close(3+1)                                                      //                                                                                  //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return isClosed;
    }

    public void abort() throws Exception {
        checkClosed();
        p.abortSelf(DESC_RM_ABORT);
    }

    public final void close() throws Exception {
        synchronized (this) {//safe close
            if (isClosed) return;
            isClosed = true;
        }
        p.recycleSelf();
    }

    void checkClosed() throws Exception {
        if (isClosed) throw new BeePooledObjectCalledException("No operations allowed after object handle closed");
    }

    //***************************************************************************************************************//
    //                                     2: handle accessed time(2+0)                                              //                                                                                  //
    //***************************************************************************************************************//
    final void setLastAccessedTime() {
        p.lastAccessTime = System.currentTimeMillis();
    }

    public final long getLastAccessedTime() throws Exception {
        checkClosed();
        return p.lastAccessTime;
    }

    //***************************************************************************************************************//
    //                                     3: Key and Proxy(2+0)                                                     //                                                                                  //
    //***************************************************************************************************************//
    public K getKey() throws Exception {
        checkClosed();
        return p.key;
    }

    public V getObject() throws Exception {
        checkClosed();
        return null;//don't expose pooled object to outside
    }

    //***************************************************************************************************************//
    //                                     4: Object call(2+0)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public Object call(String methodName) throws Exception {
        return call(methodName, EMPTY_CLASSES, EMPTY_CLASS_NAMES);
    }

    //call target object by reflection
    public Object call(String methodName, Class<?>[] types, Object[] params) throws Exception {
        checkClosed();

        //if method name list is null or method name is in the list
        if (objectMethodNameList == null || objectMethodNameList.contains(methodName)) {
            try {
                Object v = p.getMethod(methodName, types, params).invoke(raw, params);
                p.updateAccessTime();
                return v;
            } catch (Throwable e) {
                //if predicate is not null,then run eviction test
                if (predicate != null && isNotBlank(predicate.evictionTest(e)))
                    p.abortSelf(DESC_RM_BAD);
                throw e;
            }
        } else {
            return p.getMethod(methodName, types, params).invoke(raw, params);
        }
    }

    //***************************************************************************************************************//
    //                                     5: Object override(1+0)                                                   //                                                                                  //
    //***************************************************************************************************************//
    public String toString() {
        return isClosed ? "Object handle has been closed" : p.toString();
    }

    //***************************************************************************************************************//
    //                                     6: Handle Impl by proxy                                                   //                                                                                  //
    //***************************************************************************************************************//
    static class ObjectHandleImpl2<K, V> extends ObjectHandleImpl<K, V> {
        private final V objectProxy;

        ObjectHandleImpl2(PooledObject<K, V> p, BeeObjectPredicate predicate, Constructor<?> proxyClassConstructor) throws Exception {
            super(p, predicate);
            this.objectProxy = (V) proxyClassConstructor.newInstance(p, this, predicate);
        }

        @Override
        public V getObject() throws Exception {
            this.checkClosed();
            return objectProxy;
        }
    }
}
