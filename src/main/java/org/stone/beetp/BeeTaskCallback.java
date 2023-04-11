/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

/**
 * Task call back interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskCallback {

    void beforeCall(BeeTaskHandle handle);

    void afterCall(Object result, BeeTaskHandle handle);

    void afterThrowing(Throwable e, BeeTaskHandle handle);
}
