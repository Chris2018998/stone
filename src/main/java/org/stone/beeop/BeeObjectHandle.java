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
 * A handle interface of borrowed object
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectHandle {

    /**
     * Query handle state whether is closed.
     *
     * @return true that handle is closed
     */
    boolean isClosed();

    /**
     * Closes this handle object
     */
    void close() throws Exception;

    /**
     * Abort pooled object related with this handle
     */
    void abort() throws Exception;

    /**
     * Gets pooled key associated with the handle
     *
     * @return associated pooled key
     * @throws Exception if handle is closed
     */
    Object getObjectKey() throws Exception;

    /**
     * Gets last accessed time on this handle
     *
     * @return a milliseconds time value
     * @throws Exception if handle is closed
     */
    long getLassAccessedTime() throws Exception;

    /**
     * Gets proxy of borrowed object
     *
     * @return proxy if exists,otherwise return null
     * @throws Exception if handle is closed
     */
    Object getObjectProxy() throws Exception;

    /**
     * call a parameterless method of borrowed object.
     *
     * @param methodName is name of target method
     * @return an object as result of method call
     * @throws Exception when failed to call
     */
    Object call(String methodName) throws Exception;

    /**
     * call a method of borrowed object with a parameter array.
     *
     * @param methodName  is name of target method
     * @param paramTypes  is signed parameter type array of method
     * @param paramValues is parameters value array
     * @return an object as result of method call
     * @throws Exception when failed to call
     */
    Object call(String methodName, Class<?>[] paramTypes, Object[] paramValues) throws Exception;
}
