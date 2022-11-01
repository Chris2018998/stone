/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection factory
 *
 * @author Chris
 * @version 1.0
 */
public interface RawConnectionFactory {
    //create connection instance
    Connection create() throws SQLException;
}
