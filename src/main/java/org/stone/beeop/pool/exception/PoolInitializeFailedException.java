/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool.exception;

import org.stone.beeop.BeeObjectPoolException;

/**
 * pool initialize failed exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolInitializeFailedException extends BeeObjectPoolException {

    public PoolInitializeFailedException(String s) {
        super(s);
    }

    public PoolInitializeFailedException(Throwable cause) {
        super(cause);
    }
}