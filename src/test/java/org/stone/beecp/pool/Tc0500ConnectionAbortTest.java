package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0500ConnectionAbortTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(4);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testConnectionAbort() throws SQLException {


    }
}

