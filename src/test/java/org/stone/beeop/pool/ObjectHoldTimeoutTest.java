/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.object.JavaBook;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectHoldTimeoutTest extends TestCase {
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setInitialSize(0);
        config.setHoldTimeout(1000L);// hold and not using objects;
        config.setTimerCheckInterval(1000L);//one second interval
        config.setDelayTimeForNextClear(0L);
        config.setObjectClassName(JavaBook.class.getName());
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectHandle handle = null;
        try {
            //FastObjectPool pool = (FastObjectPool) TestUtil.getFieldValue(obs, "pool");
            handle = obs.getObjectHandle();
            ObjectPoolMonitorVo monitorVo = obs.getPoolMonitorVo();
            if (monitorVo.getIdleSize() + monitorVo.getUsingSize() != 1)
                TestUtil.assertError("Total objects not as expected 1");
            if (monitorVo.getUsingSize() != 1)
                TestUtil.assertError("Using objects not as expected 1");

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));

            monitorVo = obs.getPoolMonitorVo();
            int usingSize = monitorVo.getUsingSize();
            if (usingSize != 0)
                TestUtil.assertError("Using objects not as expected 0 after hold timeout,actual value:" + usingSize);
            try {
                handle.call("toString", new Class[0], new Object[0]);
                System.out.println("handle isClosed:" + handle.isClosed());

                TestUtil.assertError("must throw closed exception");
            } catch (Exception e) {
                System.out.println(e);
            }
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
        } finally {
            if (handle != null) handle.close();
        }
    }
}
