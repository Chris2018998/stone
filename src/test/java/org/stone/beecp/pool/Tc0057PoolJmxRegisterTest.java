package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0057PoolJmxRegisterTest extends TestCase {

    public void setUp() throws Exception {
        MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
    }

    public void testJmxRegister() throws Exception {
        String poolName = "test";
        BeeDataSourceConfig config = createDefault();
        config.setEnableJmx(true);
        config.setPoolName(poolName);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        String name1 = "FastConnectionPool:type=BeeCP(" + poolName + ")";
        String name2 = "BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config";
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        pool.close();
    }

    public void testJmxBeanMethods() throws SQLException {
        String poolName = "test";
        BeeDataSourceConfig config = createDefault();
        config.setPoolName(poolName);
        config.setMaxActive(10);
        config.setInitialSize(2);
        config.setBorrowSemaphoreSize(4);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(poolName, pool.getPoolName());
        Assert.assertEquals(2, pool.getTotalSize());
        Assert.assertEquals(2, pool.getIdleSize());
        Assert.assertEquals(0, pool.getUsingSize());
        Assert.assertEquals(0, pool.getSemaphoreAcquiredSize());
        Assert.assertEquals(0, pool.getSemaphoreWaitingSize());
        Assert.assertEquals(0, pool.getTransferWaitingSize());
    }

    public void testOnPrintRuntimeLog() throws SQLException {
        Assert.assertTrue(true);
        //void setPrintRuntimeLog ( boolean indicator)
    }
}


