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

import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSourceConfig;

/**
 * Mock Object Handle
 *
 * @author Chris Liao
 */

public class MockObjectHandleImpl<K, V> implements BeeObjectHandle<K, V> {

    /**
     * Get object
     *
     * @return associated pooled key
     * @throws Exception if handle is closed
     */
    public V getObject() throws Exception {
        return null;
    }

    /**
     * Get category key of object.
     *
     * @return associated pooled key
     * @throws Exception if handle is closed
     */
    public K getObjectKey() throws Exception {
        return null;
    }

    /**
     * Gets last accessed time of object.
     *
     * @return a nanoseconds time value
     * @throws Exception if handle is closed
     */
    public long getLastAccessedTime() throws Exception {
        return 0L;
    }

    /**
     * Sets last accessed time of object.
     *
     * @throws Exception if handle is closed
     */
    public void setLastAccessedTime() throws Exception {
    }


    /**
     * Get reflection proxy of pooled object.
     *
     * @return null when not configured interfaces in {@link BeeObjectSourceConfig}
     * @throws Exception if handle is closed
     */
    public V getObjectProxy() throws Exception {
        return null;
    }

    /**
     * Call a method of object without parameter.
     *
     * @param methodName is name of invocation method
     * @return result object of call
     * @throws Exception when call fail
     */
    public Object call(String methodName) throws Exception {
        return null;
    }

    /**
     * Call a method of object with array of parameters.
     *
     * @param methodName  is name of invocation method
     * @param paramTypes  is array of parameter types
     * @param paramValues is array of parameter values
     * @return result object of call
     * @throws Exception when call fail
     */
    public Object call(String methodName, Class<?>[] paramTypes, Object[] paramValues) throws Exception {
        return null;
    }


    /**
     * Query handle state whether is closed.
     *
     * @return true that is closed
     */
    public boolean isClosed() {
        return false;
    }

    /**
     * Closes this handle.
     */
    public void close() throws Exception {

    }

    /**
     * Physically close pooled object and remove it from pool.
     */
    public void abort() throws Exception {

    }
}
