package org.stone.beecp.other;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Test for scenario of database down
 * <p>
 * HikariCP-6.2.1.jar
 * stone-1.4.6.jar
 * mysql-connector-j-8.3.0.jar
 * mysql-8.4.3
 *
 * @author Chris Liao
 */

public class DbDownTest {
    private static final long maxWait = Long.MAX_VALUE;
    public static String driver = "com.mysql.cj.jdbc.Driver";
    public static String url = "jdbc:mysql://localhost/test?connectTimeout=50&socketTimeout=100";
    public static String user = "root";
    public static String password = "root";
    public static int size = 5;

    public static void main(String[] args) {
        DataSource beeDs = createBeeCP();
        DataSource hikariDs = createHikari();
        long delay = 5000L;
        long period = 2000L;
        new Timer(true).schedule(new TestTask("beeDs", beeDs), delay, period);
        new Timer(true).schedule(new TestTask("hikariDs", hikariDs), delay, period);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(300L));
    }

    private static DataSource createBeeCP() {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, user, password);
        config.setInitialSize(size);
        config.setMaxActive(size);
        config.setMaxWait(maxWait);
        config.setTimerCheckInterval(30000L);
        return new BeeDataSource(config);
    }

    private static DataSource createHikari() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setConnectionTimeout(maxWait);
        config.setMaximumPoolSize(size);
        config.setMinimumIdle(size);
        return new HikariDataSource(config);
    }

    static final class TestTask extends TimerTask {
        private final String name;
        private final DataSource ds;

        public TestTask(String name, DataSource ds) {
            this.ds = ds;
            this.name = name;
        }

        public void run() {
            Connection con = null;
            final long startTime = System.currentTimeMillis();
            try {
                con = ds.getConnection();
                PreparedStatement ps = null;
                try {
                    ps = con.prepareStatement("select 1 from dual");
                    ps.execute();
                } catch (Exception e) {
                    System.err.println("(" + name + ")-Failed to execute sql");
                } finally {
                    if (ps != null) try {
                        ps.close();
                    } catch (Exception e) {
                        System.err.println("(" + name + ")-Failed to close statement");
                    }
                }
            } catch (Throwable e) {
                System.err.println(name + "-Failed to get a connection,took time:" + (System.currentTimeMillis() - startTime) + "ms");
            } finally {
                if (con != null) try {
                    con.close();
                } catch (Exception e) {
                    System.err.println("(" + name + ")-Failed to close connection");
                }
            }
        }
    }
}
