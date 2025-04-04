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
 * filter interface on method call.
 *
 * @author Chris
 * @version 1.0
 */

public interface BeeObjectMethodFilter<K> {

    void doFilter(K key, String methodName, Class<?>[] paramTypes, Object[] paramValues) throws Exception;
}
