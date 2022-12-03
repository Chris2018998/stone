/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.object.JavaBook;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectIdleTimeoutTest extends TestCase {
    private final int initSize = 5;
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setIdleTimeout(1000);
        config.setTimerCheckInterval(1000L);// two seconds interval
        config.setDelayTimeForNextClear(1);
        config.setObjectClassName(JavaBook.class.getName());
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
//        FastObjectPool pool = (FastObjectPool) TestUtil.getFieldValue(obs, "pool");
//        CountDownLatch poolThreadLatch = (CountDownLatch) TestUtil.getFieldValue(pool, "poolThreadLatch");
//        if (poolThreadLatch.getCount() > 0) poolThreadLatch.await();

        ObjectPoolMonitorVo monitorVo = obs.getPoolMonitorVo();
        int usingSize = monitorVo.getUsingSize();
        int idleSize = monitorVo.getIdleSize();
        int totalSize = usingSize + idleSize;

        if (totalSize != initSize)
            TestUtil.assertError("Total object not as expected:" + initSize);
        if (idleSize != initSize) TestUtil.assertError("Idle object not as expected:" + initSize);

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
        monitorVo = obs.getPoolMonitorVo();
        usingSize = monitorVo.getUsingSize();
        idleSize = monitorVo.getIdleSize();
        totalSize = usingSize + idleSize;

        if (totalSize != 0)
            TestUtil.assertError("Total size not expected:" + 0);
        if (idleSize != 0) TestUtil.assertError("Idle size not expected:" + 0);
    }
}
