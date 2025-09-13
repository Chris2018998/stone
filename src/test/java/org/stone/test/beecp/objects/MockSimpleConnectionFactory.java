/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects;

import org.stone.beecp.BeeConnectionFactory;
import org.stone.test.beecp.driver.MockConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A Simple connection factory implementation
 *
 * @author Chris Liao
 */
public class MockSimpleConnectionFactory extends MockCommonBaseFactory implements BeeConnectionFactory {

    public Connection create() throws SQLException {
        return new MockConnection(properties);
    }

    public Connection create(String username, String password) throws SQLException {
        return create();
    }
}
