package org.jmin.beecp.pool;

import org.jmin.beecp.RawXaConnectionFactory;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.SQLException;

import static org.jmin.beecp.pool.ConnectionPoolStatics.isBlank;

/**
 * XaConnection Factory implementation by XADataSource
 *
 * @author Chris.liao
 * @version 1.0
 */
public class XaConnectionFactoryByDriverDs implements RawXaConnectionFactory {
    //username
    private final String username;
    //password
    private final String password;
    //usernameIsNotNull
    private final boolean useUsername;
    //driverDataSource
    private final XADataSource dataSource;

    //Constructor
    public XaConnectionFactoryByDriverDs(XADataSource dataSource, String username, String password) {
        this.dataSource = dataSource;
        this.username = username;
        this.password = password;
        useUsername = !isBlank(username);
    }

    //create one connection
    public final XAConnection create() throws SQLException {
        return this.useUsername ? this.dataSource.getXAConnection(this.username, this.password) : this.dataSource.getXAConnection();
    }
}