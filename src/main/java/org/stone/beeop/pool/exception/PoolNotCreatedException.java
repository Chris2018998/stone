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
 * pool exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolNotCreatedException extends BeeObjectPoolException {

    public PoolNotCreatedException(String s) {
        super(s);
    }
}