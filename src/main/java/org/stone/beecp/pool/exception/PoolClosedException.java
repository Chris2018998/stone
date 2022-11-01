/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool.exception;

/**
 * pool already closed exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolClosedException extends PoolBaseException {

    public PoolClosedException(String s) {
        super(s);
    }

}