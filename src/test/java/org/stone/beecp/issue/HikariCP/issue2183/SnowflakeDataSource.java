package org.stone.beecp.issue.HikariCP.issue2183;

import org.stone.beecp.BeeDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * DataSource implementation reference for #2183 of HikariCP
 */
public class SnowflakeDataSource implements DataSource {
    private BeeDataSource ds;

    SnowflakeDataSource(BeeDataSource ds) {
        this.ds = ds;
    }

    public Connection getConnection() throws SQLException {
        for (; ; ) {
            Connection con = ds.getConnection();
            SnowflakeConnectionWrapper wrapper = (SnowflakeConnectionWrapper) con;
            if (wrapper.isExpired()) {//expiration check
                wrapper.abort(null);//pool will remove this expired connection
            } else {
                return wrapper;
            }
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return ds.getConnection(username, password);
    }

    public final PrintWriter getLogWriter() throws SQLException {
        return ds.getLogWriter();
    }

    public final void setLogWriter(PrintWriter out) throws SQLException {
        ds.setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return ds.getLoginTimeout();
    }

    public final void setLoginTimeout(int seconds) throws SQLException {
        ds.setLoginTimeout(seconds);
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return ds.getParentLogger();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return ds.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return ds.isWrapperFor(iface);
    }
}
