/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop;

/**
 * object handle interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectHandle<E> {

    void close() throws Exception;

    boolean isClosed() throws Exception;

    E getObjectProxy() throws Exception;

    Object call(String methodName) throws Exception;

    Object call(String methodName, Class[] paramTypes, Object[] paramValues) throws Exception;
}
