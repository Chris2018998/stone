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
    private final Object raw;
    private final PooledObject<K, V> p;
    private final BeeObjectPredicate predicate;
    private final List<String> objectMethodNameList;
    private boolean isClosed;

    ObjectHandleImpl(PooledObject<K, V> p, BeeObjectPredicate predicate) {
        this.p = p;
        this.raw = p.raw;
        p.handleInUsing = this;
        this.predicate = predicate;
        this.objectMethodNameList = p.objectMethodNameList;
    }

    //***************************************************************************************************************//
    //                                  1: override methods(6)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return isClosed;
    }

    public String toString() {
        return p.toString();
    }

    public final long getLastAccessedTime() throws Exception {
        checkClosed();
        return p.lastAccessTime;
    }

    final void setLastAccessedTime() throws Exception {
        checkClosed();
        p.lastAccessTime = System.currentTimeMillis();
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

    //***************************************************************************************************************//
    //                                 2: raw methods call methods(2)                                                //                                                                                  //
    //***************************************************************************************************************//

    public K getKey() throws Exception {
        checkClosed();
        return p.key;
    }

    public V getObject() throws Exception {
        checkClosed();
        return p.raw;
    }

    public V getObjectProxy() throws Exception {
        checkClosed();
        return null;
    }

    public Object call(String methodName) throws Exception {
        return call(methodName, EMPTY_CLASSES, EMPTY_CLASS_NAMES);
    }

    public Object call(String name, Class<?>[] types, Object[] params) throws Exception {
        checkClosed();
        boolean existInList = objectMethodNameList == null || objectMethodNameList.contains(name);

        try {
            Object v = p.getMethod(name, types, params).invoke(raw, params);
            if (existInList) p.updateAccessTime();

            return v;
        } catch (Exception e) {
            if (existInList && predicate != null && isNotBlank(predicate.evictionTest(e)))
                p.abortSelf(DESC_RM_BAD);

            throw e;
        }
    }

    void checkClosed() throws Exception {
        if (isClosed) throw new BeePooledObjectCalledException("No operations allowed after object handle closed");
    }

    static class ObjectHandleImpl2<K, V> extends ObjectHandleImpl<K, V> {
        private final V objectProxy;

        ObjectHandleImpl2(PooledObject<K, V> p, BeeObjectPredicate predicate, Constructor<?> proxyClassConstructor) throws Exception {
            super(p, predicate);
            this.objectProxy = (V) proxyClassConstructor.newInstance(p, this, predicate);
        }

        @Override
        public V getObjectProxy() throws Exception {
            this.checkClosed();
            return objectProxy;
        }
    }
}
