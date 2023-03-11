/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop;

/**
 * pool exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectPoolException extends Exception {

    public BeeObjectPoolException(String s) {
        super(s);
    }

    public BeeObjectPoolException(Throwable cause) {
        super(cause);
    }
}