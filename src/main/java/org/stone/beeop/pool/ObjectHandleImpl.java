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
import org.stone.beeop.exception.BeePooledObjectException;

import static org.stone.beeop.pool.ObjectPoolStatics.DESC_RM_ABORT;

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

    //***************************************************************************************************************//
    //                                     3: Method call(2+0)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public Object call(String methodName) throws Throwable {
        checkClosed();
        return p.callMethod(methodName, null, null);
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
        if (isClosed) throw new BeePooledObjectException("No operations allowed after object handle closed");
    }
}
