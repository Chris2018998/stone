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
import org.stone.beeop.exception.BeePooledObjectCallException;

import java.lang.reflect.Constructor;

import static org.stone.beeop.pool.ObjectPoolStatics.*;

/**
 * object Handle implement
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public class ObjectHandleImpl<K, V> implements BeeObjectHandle<K, V> {
    protected final Object instance;
    protected final PooledObject<K, V> p;
    private boolean isClosed;

    ObjectHandleImpl(PooledObject<K, V> p) {
        this.p = p;
        this.instance = p.objectInstance;
        p.handleInUsing = this;
    }

    //***************************************************************************************************************//
    //                                     1: Handle close(2+0)                                                      //                                                                                  //
    //***************************************************************************************************************//
    public final void close() throws Exception {
        synchronized (this) {//safe close
            if (isClosed) return;
            isClosed = true;
        }
        p.recycleSelf();
    }

    public void abort() throws Exception {
        synchronized (this) {//safe close
            if (isClosed) return;
            isClosed = true;
        }
        p.abortSelf(DESC_RM_ABORT);
    }

    //***************************************************************************************************************//
    //                                     2: Key and Proxy(2+0)                                                     //                                                                                  //
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
    //                                     3: Method call(2+0)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public Object call(String methodName) throws Throwable {
        checkClosed();
        return p.callMethod(methodName, EMPTY_CLASSES, EMPTY_CLASS_NAMES);
    }

    public Object call(String methodName, Class<?>[] types, Object[] params) throws Throwable {
        checkClosed();
        return p.callMethod(methodName, types, params);
    }

    //***************************************************************************************************************//
    //                                     4: Object Monitoring(3+1)                                                   //                                                                                  //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return isClosed;
    }

    public String toString() {
        return isClosed ? "Object handle has been closed" : p.toString();
    }

    public final long getLastAccessedTime() throws Exception {
        checkClosed();
        return p.lastAccessTime;
    }

    void checkClosed() throws Exception {
        if (isClosed) throw new BeePooledObjectCallException("No operations allowed after object handle closed");
    }

    //***************************************************************************************************************//
    //                                     5: Handle Impl by proxy                                                   //                                                                                  //
    //***************************************************************************************************************//
    static class ObjectHandleImpl2<K, V> extends ObjectHandleImpl<K, V> {
        private final V objectProxy;

        ObjectHandleImpl2(PooledObject<K, V> p, BeeObjectPredicate predicate, Constructor<?> proxyClassConstructor) throws Exception {
            super(p);
            this.objectProxy = (V) proxyClassConstructor.newInstance(p, this, predicate);
        }

        @Override
        public V getObject() throws Exception {
            this.checkClosed();
            return objectProxy;
        }
    }
}
