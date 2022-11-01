/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.TestCase;
import org.stone.beeop.TestUtil;
import org.stone.beeop.object.JavaBook;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class PoolRestTest extends TestCase {
    private final int initSize = 5;
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setInitialSize(initSize);
        config.setObjectClassName(JavaBook.class.getName());
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        ObjectPoolMonitorVo monitorVo = obs.getPoolMonitorVo();
        int usingSize = monitorVo.getUsingSize();
        int idleSize = monitorVo.getIdleSize();
        int totalSize = usingSize + idleSize;

        if (totalSize != initSize)
            TestUtil.assertError("Total size expected:%s,current is:%s", initSize, totalSize);
        if (idleSize != initSize)
            TestUtil.assertError("idle expected:%s,current is:%s", initSize, idleSize);

        obs.clear();

        monitorVo = obs.getPoolMonitorVo();
        usingSize = monitorVo.getUsingSize();
        idleSize = monitorVo.getIdleSize();
        totalSize = usingSize + idleSize;
        if (totalSize != 0)
            TestUtil.assertError("Total size not as expected 0,but current is:%s", totalSize, "");
        if (idleSize != 0)
            TestUtil.assertError("Idle size not as expected 0,but current is:%s", idleSize, "");
    }
}
