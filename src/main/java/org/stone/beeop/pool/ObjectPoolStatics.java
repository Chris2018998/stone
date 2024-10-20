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

/**
 * Pool Static Center
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ObjectPoolStatics {
    public static final Class[] EMPTY_CLASSES = new Class[0];
    public static final String[] EMPTY_CLASS_NAMES = new String[0];
    //pool object state
    static final int OBJECT_CLOSED = 0;
    static final int OBJECT_CREATING = 1;
    static final int OBJECT_IDLE = 2;
    static final int OBJECT_USING = 3;

    //pool state
    static final int POOL_NEW = 0;
    static final int POOL_STARTING = 1;
    static final int POOL_READY = 2;
    static final int POOL_CLOSING = 3;
    static final int POOL_CLOSED = 4;
    static final int POOL_CLEARING = 5;

    //pool thread state
    static final int THREAD_WORKING = 0;
    static final int THREAD_WAITING = 1;
    //static final int THREAD_EXIT = 2;

    //remove reason
    static final String DESC_RM_INIT = "init";
    static final String DESC_RM_BAD = "bad";
    static final String DESC_RM_ABORT = "abort";
    static final String DESC_RM_IDLE = "idle";
    static final String DESC_RM_CLOSED = "closed";
    static final String DESC_RM_CLEAR = "clear";
    static final String DESC_RM_DESTROY = "destroy";

    //***************************************************************************************************************//
    //                               1: Handle close methods(1)                                                  //
    //***************************************************************************************************************//
    static void tryCloseObjectHandle(BeeObjectHandle handle) {
        try {
            handle.close();
        } catch (Throwable e) {
            //do nothing
        }
    }
}

