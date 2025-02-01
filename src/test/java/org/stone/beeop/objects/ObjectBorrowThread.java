/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.objects;

import org.stone.base.TestUtil;
import org.stone.beeop.BeeKeyedObjectPool;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;

/**
 * Object borrow thread
 *
 * @author Chris Liao
 */
public class ObjectBorrowThread extends Thread {
    private final BeeObjectSource os;
    private final BeeKeyedObjectPool pool;
    private final Object objectKey;
    private BeeObjectHandle objectHale;
    private Exception failureCause;

    public ObjectBorrowThread(BeeObjectSource os) {
        this(os, null, null);
    }

    public ObjectBorrowThread(BeeKeyedObjectPool pool) {
        this(null, pool, null);
    }

    public ObjectBorrowThread(BeeObjectSource os, BeeKeyedObjectPool pool, Object objectKey) {
        this.os = os;
        this.pool = pool;
        this.objectKey = objectKey;
        this.setDaemon(true);
    }

    public Exception getFailureCause() {
        return failureCause;
    }

    public BeeObjectHandle getObjectHandle() {
        return objectHale;
    }

    public void run() {
        try {
            if (os != null) {
                if (objectKey != null) {
                    objectHale = os.getObjectHandle(objectKey);
                } else {
                    objectHale = os.getObjectHandle();
                }

            } else {
                if (objectKey != null) {
                    objectHale = pool.getObjectHandle(objectKey);
                } else {
                    objectHale = pool.getObjectHandle();
                }
            }
        } catch (Exception e) {
            this.failureCause = e;
        } finally {
            if (objectHale != null) TestUtil.oclose(objectHale);
        }
    }
}