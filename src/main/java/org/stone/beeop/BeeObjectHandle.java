/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

/**
 * Handle interface represents wrapper of borrowed object.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectHandle<K, V> {

    /**
     * Get object
     *
     * @return associated pooled key
     * @throws Exception if handle is closed
     */
    V getObject() throws Exception;

    /**
     * Get category key of object.
     *
     * @return associated pooled key
     * @throws Exception if handle is closed
     */
    K getObjectKey() throws Exception;

    /**
     * Get reflection proxy of pooled object.
     *
     * @return null when not configured interfaces in {@link BeeObjectSourceConfig}
     * @throws Exception if handle is closed
     */
    V getObjectProxy() throws Exception;

    /**
     * Gets last accessed time of object.
     *
     * @return a milliseconds time value
     * @throws Exception if handle is closed
     */
    long getLastAccessedTime() throws Exception;

    /**
     * Call a method of object without parameter.
     *
     * @param methodName is name of invocation method
     * @return result object of call
     * @throws Exception when call fail
     */
    Object call(String methodName) throws Exception;

    /**
     * Call a method of object with array of parameters.
     *
     * @param methodName  is name of invocation method
     * @param paramTypes  is array of parameter types
     * @param paramValues is array of parameter values
     * @return result object of call
     * @throws Exception when call fail
     */
    Object call(String methodName, Class<?>[] paramTypes, Object[] paramValues) throws Exception;

    /**
     * Query handle state whether is closed.
     *
     * @return true that is closed
     */
    boolean isClosed();

    /**
     * Closes this handle.
     */
    void close() throws Exception;

    /**
     * Physically close pooled object and remove it from pool.
     */
    void abort() throws Exception;
}
