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
import org.stone.beeop.RawObjectFactory;
import org.stone.beeop.object.Book;
import org.stone.beeop.object.JavaBookFactory;

import java.util.concurrent.TimeUnit;

public class PoolRestartWithConfigTest extends TestCase {
    private final int initSize = 10;
    private final long delayTimeForNextClear = TimeUnit.SECONDS.toMillis(10);//10 Seconds
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setObjectInterfaces(new Class[]{Book.class});
        config.setObjectFactory(new JavaBookFactory());
        config.setDelayTimeForNextClear(delayTimeForNextClear);//Ms
        obs = new BeeObjectSource(config);
    }

    public void test() throws Throwable {
        BeeObjectHandle handle1 = obs.getObjectHandle();
        Object object1 = handle1.getObjectProxy();
        if (!(object1 instanceof Book)) TestUtil.assertError("Failed to get object");

        BeeObjectSourceConfig config2 = new BeeObjectSourceConfig();
        config2.setInitialSize(1);
        config2.setMaxActive(5);
        config2.setObjectFactory(new StringFactory());
        config2.setObjectInterfaces(new Class[]{CharSequence.class});
        config2.setDelayTimeForNextClear(delayTimeForNextClear);//Ms
        obs.restartPool(true, config2);
        ObjectPoolMonitorVo vo = obs.getPoolMonitorVo();
        TestUtil.assertError("pool idle size expect value:%s,actual value:%s", 1, vo.getIdleSize());
        TestUtil.assertError("pool max size expect value:%s,actual value:%s", 5, vo.getPoolMaxSize());

        BeeObjectHandle handle2 = obs.getObjectHandle();
        Object object2 = handle2.getObjectProxy();
        if (!(object2 instanceof CharSequence)) TestUtil.assertError("Failed to get object");
        handle2.close();
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    class StringFactory implements RawObjectFactory {
        public Object create() throws Exception {
            return "Java核心技术·卷1";
        }

        public void setDefault(Object obj) throws Exception {
            //do nothingr
        }

        public void reset(Object obj) throws Exception {
            //do nothing
        }

        public void destroy(Object obj) {
            //do nothing
        }

        public boolean isValid(Object obj, int timeout) {
            return true;
        }
    }
}
