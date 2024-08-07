/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import javax.sql.XAConnection;
import java.sql.SQLException;

/**
 * A XAConnection factory interface,whose implementation are used to create XAConnections for pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeXaConnectionFactory {

    /**
     * Creates a jdbc xa connection.
     *
     * @return a xa connection
     * @throws SQLException when creates failed(maybe invalid url,error username and password and so on)
     */
    XAConnection create() throws SQLException;
}
