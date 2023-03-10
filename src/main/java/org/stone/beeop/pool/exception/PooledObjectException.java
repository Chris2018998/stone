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
public class PooledObjectException extends Exception {
    public PooledObjectException(String message) {
        super(message);
    }

    public PooledObjectException(Throwable cause) {
        super(cause);
    }
}