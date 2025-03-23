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

import static org.stone.tools.BeanUtil.CommonLog;

/**
 * Pool Static Center
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ObjectPoolStatics {
    //config name of properties of object factory
    public static final String CONFIG_FACTORY_PROP = "factoryProperties";
    //config name of properties count of object factory
    public static final String CONFIG_FACTORY_PROP_SIZE = "factoryProperties.size";
    //properties prefix of object factory
    public static final String CONFIG_FACTORY_PROP_KEY_PREFIX = "factoryProperties.";
    //config name of object interfaces
    public static final String CONFIG_OBJECT_INTERFACES = "objectInterfaces";
    //config name of object interface class names
    public static final String CONFIG_OBJECT_INTERFACE_NAMES = "objectInterfaceNames";
    //config name of exclusion list of config print
    public static final String CONFIG_CONFIG_PRINT_EXCLUSION_LIST = "configPrintExclusionList";

    public static final Class[] EMPTY_CLASSES = new Class[0];
    public static final String[] EMPTY_CLASS_NAMES = new String[0];
    //pool state
    public static final int POOL_NEW = 0;
    public static final int POOL_STARTING = 1;
    public static final int POOL_READY = 2;
    public static final int POOL_CLOSING = 3;
    public static final int POOL_CLOSED = 4;
    public static final int POOL_CLEARING = 5;
    //pool object state
    static final int OBJECT_CLOSED = 0;
    static final int OBJECT_IDLE = 1;
    static final int OBJECT_CREATING = 2;
    static final int OBJECT_BORROWED = 3;
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

    public static void oclose(BeeObjectHandle h) {
        try {
            h.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing object handle", e);
        }
    }
}

