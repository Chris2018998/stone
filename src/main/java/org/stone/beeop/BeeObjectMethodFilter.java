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
 * A filter interface of method invocation on pooled object
 *
 * @author Chris
 * @version 1.0
 */

public interface BeeObjectMethodFilter {

    void doFilter(Object key, String methodName, Class<?>[] paramTypes, Object[] paramValues) throws Exception;
}
