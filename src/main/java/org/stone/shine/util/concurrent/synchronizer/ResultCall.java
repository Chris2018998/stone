/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer;

/**
 * Object work as a plugin of class {@code ResultWaitPool}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ResultCall {

    //do some thing(don't park thread in implementation method)
    Object call(Object arg) throws Exception;
}
