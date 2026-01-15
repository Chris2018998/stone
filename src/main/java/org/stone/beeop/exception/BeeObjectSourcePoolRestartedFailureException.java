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
 * pool already closed exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourcePoolRestartedFailureException extends BeeObjectSourcePoolException {

    public BeeObjectSourcePoolRestartedFailureException(String s) {
        super(s);
    }

    public BeeObjectSourcePoolRestartedFailureException(String s, Throwable cause) {
        super(s, cause);
    }

}