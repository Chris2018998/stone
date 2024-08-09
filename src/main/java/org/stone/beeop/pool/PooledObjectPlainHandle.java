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
import org.stone.beeop.pool.exception.ObjectCallException;

import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * object Handle implement
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PooledObjectPlainHandle implements BeeObjectHandle {
    private final Object raw;
    private final PooledObject p;
    private final BeeObjectPredicate predicate;
    private boolean isClosed;

    PooledObjectPlainHandle(PooledObject p, BeeObjectPredicate predicate) {
        this.p = p;
        this.raw = p.raw;
        p.handleInUsing = this;
        this.predicate = predicate;
    }

    //***************************************************************************************************************//
    //                                  1: override methods(5)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return isClosed;
    }

    public String toString() {
        return p.toString();
    }

    public final long getLassAccessedTime() throws Exception {
        checkClosed();
        return p.lastAccessTime;
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
    public Object getObjectKey() throws Exception {
        checkClosed();
        return p.key;
    }

    public Object getObjectProxy() throws Exception {
        checkClosed();
        return null;
    }

    public Object call(String methodName) throws Exception {
        return call(methodName, EMPTY_CLASSES, EMPTY_CLASS_NAMES);
    }

    public Object call(String name, Class<?>[] types, Object[] params) throws Exception {
        checkClosed();

        try {
            Object v = p.getMethod(name, types, params).invoke(raw, params);
            p.updateAccessTime();
            return v;
        } catch (Exception e) {
            if (predicate != null && isNotBlank(predicate.evictTest(e)))
                p.abortSelf(DESC_RM_BAD);

            throw e;
        }
    }

    void checkClosed() throws Exception {
        if (isClosed) throw new ObjectCallException("No operations allowed after object handle closed");
    }
}
