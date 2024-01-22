/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.pool.exception;

/**
 * Pool Exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskPoolException extends Exception {

    public TaskPoolException(String message) {
        super(message);
    }

    public TaskPoolException(Throwable cause) {
        super(cause);
    }

}