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
 * configuration exception.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfigException extends RuntimeException {

    public BeeObjectSourceConfigException(String s) {
        super(s);
    }

    public BeeObjectSourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
