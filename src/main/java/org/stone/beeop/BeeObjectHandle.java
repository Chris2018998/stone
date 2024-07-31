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

    boolean isClosed();

    Object getObjectKey();

    long getCreationTime();

    long getLassAccessTime();

    void close() throws Exception;

    Object getObjectProxy() throws Exception;

    Object call(String methodName) throws Exception;

    Object call(String methodName, Class[] paramTypes, Object[] paramValues) throws Exception;
}
