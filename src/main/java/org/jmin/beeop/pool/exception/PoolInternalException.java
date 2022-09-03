/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop.pool.exception;

/**
 * pool exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolInternalException extends PoolBaseException {

    public PoolInternalException(String s) {
        super(s);
    }

    public PoolInternalException(Throwable cause) {
        super(cause);
    }

    public PoolInternalException(String message, Throwable cause) {
        super(message, cause);
    }

}