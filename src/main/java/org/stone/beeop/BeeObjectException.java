/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

/**
 * pooled object exception.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectException extends Exception {
    public BeeObjectException(String message) {
        super(message);
    }

    public BeeObjectException(Throwable cause) {
        super(cause);
    }

    public BeeObjectException(String message, Throwable cause) {
        super(message, cause);
    }

}