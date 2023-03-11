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

import org.stone.beeop.BeeObjectPoolException;

/**
 * pool already closed exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolInClearingException extends BeeObjectPoolException {
    public PoolInClearingException(String s) {
        super(s);
    }
}