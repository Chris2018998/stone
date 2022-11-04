/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beecp.pool.exception;

import java.sql.SQLException;

/**
 * if test sql execute failed,then throws this exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TestSQLFailException extends SQLException {

    public TestSQLFailException(String s) {
        super(s);
    }

    public TestSQLFailException(Throwable cause) {
        super(cause);
    }

    public TestSQLFailException(String message, Throwable cause) {
        super(message, cause);
    }
}