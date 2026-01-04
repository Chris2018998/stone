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
 * Handle interface of borrowed object.
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectHandle<K, V> extends AutoCloseable {

    /**
     * Get pooled key.
     *
     * @return associated pooled key
     * @throws Exception if handle is closed
     */
    K getKey() throws Exception;

    /**
     * Get wrapper object of borrowed object
     *
     * @return wrapper object,which maybe null when not
     * @throws Exception if handle is closed
     */
    V getObject() throws Exception;

    /**
     * Close handle
     */
    void close() throws Exception;

    /**
     * Query handle state whether is closed.
     *
     * @return true that is closed
     */
    boolean isClosed();

    /**
     * Physically close pooled object and remove it from pool.
     */
    void abort() throws Exception;

    /**
     * Gets last accessed time of method call on object.
     *
     * @return a nanoseconds time value
     * @throws Exception if handle is closed
     */
    long getLastAccessedTime() throws Exception;

    /**
     * Call a method on object.
     *
     * @param methodName is name of invocation method
     * @return result object of call
     * @throws Exception when call fail
     */
    Object call(String methodName) throws Exception;

    /**
     * Call a method on object with parameters.
     *
     * @param methodName  is name of invocation method
     * @param paramTypes  is array of parameter types
     * @param paramValues is array of parameter values
     * @return result object of call
     * @throws Exception when call fail
     */
    Object call(String methodName, Class<?>[] paramTypes, Object[] paramValues) throws Exception;

}
