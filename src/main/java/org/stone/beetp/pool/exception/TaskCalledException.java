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

import org.stone.beetp.BeeTaskException;

/**
 * Task Exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskCalledException extends BeeTaskException {

    public TaskCalledException(String message) {
        super(message);
    }

    public TaskCalledException(Throwable cause) {
        super(cause);
    }

}