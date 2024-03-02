package org.stone.beecp.issue.HikariCP.issue2183;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Springboot Configuration file
 */
//@Configuration
public class SnowflakeDataSourceConfig {

    //@Bean
    public DataSource SnowflakeDataSource() {
        String userName = "";//put your userName
        String jdbcUrl = "";//put your url
        String driverName = "";//put your driver class name

        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUrl(jdbcUrl);
        config.setDriverClassName(driverName);
        long timeout = TimeUnit.MINUTES.toMillis(15);
        config.setIdleTimeout(timeout);
        config.setHoldTimeout(timeout);
        config.setRawConnectionFactory(new SnowflakeConnectionFactory(userName, jdbcUrl));

        return new SnowflakeDataSource(new BeeDataSource(config));
    }
}
