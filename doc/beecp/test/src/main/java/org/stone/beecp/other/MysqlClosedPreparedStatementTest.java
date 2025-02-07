package org.stone.beecp.other;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Test on Closed PreparedStatement of mysql
 * HikariCP-6.2.1.jar
 * stone-1.4.6.jar
 * mysql-connector-j-8.3.0.jar
 * mysql-8.4.3
 * <p>
 * Table sql: CREATE TABLE TEST_USER( USER_ID VARCHAR(10), USER_NAME VARCHAR(10) );
 * Table data: INSERT INTO TEST_USER VALUES('1090','test1');
 *
 * @author Chris Liao
 */
public class MysqlClosedPreparedStatementTest {

    public static void main(String[] args) throws SQLException {
        String userName = "root";
        String password = "root";
        String driverClassName = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost/test?serverTimezone=UTC";

        //1: test on HikariCP
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(userName);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setAutoCommit(false);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        testOnClosedPreparedStatement(new HikariDataSource(hikariConfig), "HikariDataSource");

        //2: test on BeeCP
        BeeDataSourceConfig beeConfig = new BeeDataSourceConfig();
        beeConfig.setJdbcUrl(url);
        beeConfig.setUsername(userName);
        beeConfig.setPassword(password);
        beeConfig.setDriverClassName(driverClassName);
        beeConfig.setDefaultAutoCommit(Boolean.FALSE);
        beeConfig.addConnectProperty("cachePrepStmts", "true");
        beeConfig.addConnectProperty("useServerPrepStmts", "true");
        testOnClosedPreparedStatement(new BeeDataSource(beeConfig), "BeeDataSource");
    }

    private static void testOnClosedPreparedStatement(DataSource ds, String dsName) throws SQLException {
        ResultSet rs = null;
        Connection con = null;

        try {
            con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement("select * from TEST_USER");
            rs = pst.executeQuery();
            pst.close();

            try {
                if (rs.next())
                    System.err.println("(" + dsName + ")Operation Could be continued on a closed PreparedStatement");
            } catch (Exception e) {
                System.out.println("(" + dsName + ")An error occurred when operation on a closed PreparedStatement");
            }
        } finally {
            if (rs != null) rs.close();
            if (con != null) con.close();
        }
    }
}
