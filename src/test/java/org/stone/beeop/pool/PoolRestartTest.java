/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.object.JavaBook;
import org.stone.beeop.object.JavaBookFactory;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class PoolRestartTest extends TestCase {
    private final int initSize = 5;
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setInitialSize(initSize);
        config.setObjectFactoryClassName(JavaBookFactory.class.getName());
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectPoolMonitorVo monitorVo = obs.getPoolMonitorVo();
        int usingSize = monitorVo.getUsingSize();
        int idleSize = monitorVo.getIdleSize();
        int totalSize = usingSize + idleSize;

        TestUtil.assertError("Total size expected:%s,current is:%s", initSize, totalSize);
        TestUtil.assertError("idle expected:%s,current is:%s", initSize, idleSize);
        obs.clear(false);

        monitorVo = obs.getPoolMonitorVo();
        usingSize = monitorVo.getUsingSize();
        idleSize = monitorVo.getIdleSize();
        totalSize = usingSize + idleSize;
        TestUtil.assertError("Total size not as expected:%s,but current is:%s", 0, totalSize);
        TestUtil.assertError("Idle size not as expected:%s,but current is:%s", 0, idleSize);
    }
}
