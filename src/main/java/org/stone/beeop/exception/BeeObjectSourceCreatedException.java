/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.exception;

import org.stone.beeop.BeeObjectSource;

/**
 * A runtime exception thrown when fail to create {@link BeeObjectSource}
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceCreatedException extends RuntimeException {

    public BeeObjectSourceCreatedException(Throwable cause) {
        super(cause);
    }
}
