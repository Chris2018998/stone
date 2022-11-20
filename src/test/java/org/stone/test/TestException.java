/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.test;

/**
 * Test Exception
 *
 * @author chris liao
 */
public class TestException extends Exception {

    public TestException(String s) {
        super(s);
    }

    public TestException(String message, Throwable cause) {
        super(message, cause);
    }

}
