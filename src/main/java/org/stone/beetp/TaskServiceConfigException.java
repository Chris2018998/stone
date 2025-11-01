/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp;

/**
 * configuration exception.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskServiceConfigException extends RuntimeException {

    public TaskServiceConfigException(String s) {
        super(s);
    }

    public TaskServiceConfigException(Throwable cause) {
        super(cause);
    }

    public TaskServiceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
