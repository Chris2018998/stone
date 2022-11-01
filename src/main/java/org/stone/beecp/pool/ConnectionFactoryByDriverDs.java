/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import org.stone.beecp.RawConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.pool.ConnectionPoolStatics.isBlank;

/**
 * Raw connection factory by dsnode implementation in driver package
 *
 * @author Chris.liao
 * @version 1.0
 */
public class ConnectionFactoryByDriverDs implements RawConnectionFactory {
    //username
    private final String username;
    //password
    private final String password;
    //usernameIsNotNull
    private final boolean useUsername;
    //driverDataSource
    private final DataSource driverDataSource;

    //Constructor
    public ConnectionFactoryByDriverDs(DataSource driverDataSource, String username, String password) {
        this.driverDataSource = driverDataSource;
        this.username = username;
        this.password = password;
        this.useUsername = !isBlank(username);
    }

    //create one connection
    public final Connection create() throws SQLException {
        return this.useUsername ? this.driverDataSource.getConnection(this.username, this.password) : this.driverDataSource.getConnection();
    }
}
