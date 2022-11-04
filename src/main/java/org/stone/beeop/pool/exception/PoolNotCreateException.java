/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop.pool.exception;

/**
 * pool exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolNotCreateException extends PoolBaseException {

    public PoolNotCreateException(String s) {
        super(s);
    }

    public PoolNotCreateException(Throwable cause) {
        super(cause);
    }

    public PoolNotCreateException(String message, Throwable cause) {
        super(message, cause);
    }

}