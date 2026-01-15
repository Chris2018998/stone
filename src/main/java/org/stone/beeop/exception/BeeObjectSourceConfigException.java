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

/**
 * Throws this runtime exception when set invalid values to {@link org.stone.beeop.BeeObjectSourceConfig} or it checks failed.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfigException extends RuntimeException {

    public BeeObjectSourceConfigException(String s) {
        super(s);
    }

    public BeeObjectSourceConfigException(Throwable cause) {
        super(cause);
    }

    public BeeObjectSourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
