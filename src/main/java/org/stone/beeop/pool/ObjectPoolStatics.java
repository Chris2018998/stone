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
import org.stone.beeop.BeeObjectPool;
import org.stone.beeop.exception.BeeObjectSourcePoolHasClosedException;
import org.stone.beeop.exception.BeeObjectSourcePoolLazyInitializationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.stone.tools.LogPrinter.DefaultLogPrinter;

/**
 * Pool Static Center
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ObjectPoolStatics {
    //config name of properties of object factory
    public static final String CONFIG_POOL_NAME_INDEX = "PoolNameIndex";
    //config name of properties of object factory
    public static final String CONFIG_FACTORY_PROP = "objectFactoryProperties";
    //config name of properties count of object factory
    public static final String CONFIG_FACTORY_PROP_SIZE = "objectFactoryProperties.size";
    //properties prefix of object factory
    public static final String CONFIG_FACTORY_PROP_KEY_PREFIX = "objectFactoryProperties.";
    //config name of exclusion list of config print
    public static final String CONFIG_EXCLUSION_LIST_OF_PRINT = "exclusionListOfPrint";
    //config name of exclusion list of config print
    public static final String CONFIG_OBJECT_METHOD_LIST = "objectMethodNameList";

    public static final Class<?>[] EMPTY_CLASSES = new Class[0];

    //pool state
    public static final int POOL_LAZY = -1;
    public static final int POOL_NEW = 0;
    public static final int POOL_STARTING = 1;
    public static final int POOL_READY = 2;
    public static final int POOL_CLOSING = 3;
    public static final int POOL_CLOSED = 4;
    public static final int POOL_RESTARTING = 5;
    public static final int POOL_RESTART_FAILED = 6;
    public static final int POOL_SUSPENDED = 7;

    //pool object state
    static final int OBJECT_CLOSED = 0;
    static final int OBJECT_IDLE = 1;
    static final int OBJECT_CREATING = 2;
    static final int OBJECT_BORROWED = 3;
    //pool thread state
    static final int THREAD_WORKING = 0;
    static final int THREAD_WAITING = 1;
    static final int THREAD_EXIT = 2;

    //remove reason
    static final String DESC_RM_POOL_INIT = "init";
    static final String DESC_RM_BAD = "bad";
    static final String DESC_RM_ABORT = "abort";
    static final String DESC_RM_IDLE = "idle";
    static final String DESC_RM_CLOSED = "closed";
    static final String DESC_RM_POOL_CLEAR = "pool_restart";
    static final String DESC_RM_POOL_SHUTDOWN = "pool_shutdown";

    @SuppressWarnings("unchecked")
    public static <K, V> BeeObjectPool<K, V> createDummyPoolImpl(boolean closedPool) {
        return (BeeObjectPool<K, V>) Proxy.newProxyInstance(
                BeeObjectPool.class.getClassLoader(),
                new Class[]{BeeObjectPool.class},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();
                        if ("isClosed".equals(methodName)) {
                            return Boolean.TRUE;
                        } else if ("toString".equals(methodName)) {
                            return closedPool ? getPoolStateDesc(POOL_CLOSED) : getPoolStateDesc(POOL_LAZY);
                        } else if (closedPool) {
                            throw new BeeObjectSourcePoolHasClosedException("No operations allowed on closed pool");
                        } else {
                            throw new BeeObjectSourcePoolLazyInitializationException("No operations allowed on lazy pool");
                        }
                    }
                }
        );
    }

    static String getPoolStateDesc(int state) {
        return switch (state) {
            case POOL_LAZY ->
                    "Pool is lazy and it can be initialized by calling getObjectHandle method of objectSource";
            case POOL_NEW -> "Pool is new";
            case POOL_STARTING -> "Pool is starting";
            case POOL_READY -> "Pool is ready";
            case POOL_CLOSING -> "Pool is closing";
            case POOL_CLOSED -> "Pool has closed";
            case POOL_RESTARTING -> "Pool is restarting";
            case POOL_RESTART_FAILED -> "Pool has restarted failed";
            case POOL_SUSPENDED -> "Pool has suspended";
            default -> "Pool is unknown state";
        };
    }

    //***************************************************************************************************************//
    //                               1: Handle close methods(1)                                                  //
    //***************************************************************************************************************//
    public static <K, V> void oclose(BeeObjectHandle<K, V> h) {
        try {
            h.close();
        } catch (Throwable e) {
            DefaultLogPrinter.debug("Warning:Error at closing object handle", e);
        }
    }
}

