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
 * Throws this runtime exception when failed to create internal pool or failed to start up its internal pool
 * in constructor of {@link BeeObjectSource}.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceCreationException extends RuntimeException {

    public BeeObjectSourceCreationException(Throwable cause) {
        super(cause);
    }
}
