/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.base;

/**
 * Object work as a plugin class to @link{ResultWaitPool}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ResultCall {

    //do some thing
    Object call(Object arg) throws Exception;
}
