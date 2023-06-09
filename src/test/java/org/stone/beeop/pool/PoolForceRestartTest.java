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
import org.stone.beeop.object.JavaBookFactory;

import java.util.concurrent.TimeUnit;

public class PoolForceRestartTest extends TestCase {
    private final int initSize = 10;
    private final long delayTimeForNextClear = TimeUnit.SECONDS.toMillis(10);//10 Seconds
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setObjectFactoryClassName(JavaBookFactory.class.getName());
        config.setDelayTimeForNextClear(delayTimeForNextClear);//Ms
        obs = new BeeObjectSource(config);
    }

    public void testForceClear() throws Throwable {
        long time1 = System.currentTimeMillis();
        obs.clear(true);
        long tookTime = System.currentTimeMillis() - time1;
        if (tookTime > delayTimeForNextClear) {
            TestUtil.assertError("Pool force clear test failed");
        } else {
            System.out.println("Pool force clear parkTime: " + tookTime + "ms");
        }
    }

    public void tearDown() throws Throwable {
        obs.close();
    }
}
