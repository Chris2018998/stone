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

import java.util.concurrent.CountDownLatch;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectGetTimeoutTest extends TestCase {
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setMaxActive(1);
        config.setMaxWait(3000);
        config.setObjectClass(JavaBook.class);
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectHandle handle = null;
        try {
            handle = obs.getObjectHandle();
            CountDownLatch lacth = new CountDownLatch(1);
            TestThread testTh = new TestThread(lacth);
            testTh.start();

            lacth.await();
            if (testTh.e == null)
                TestUtil.assertError("Object get timeout");
            else
                System.out.println(testTh.e);
        } finally {
            if (handle != null)
                handle.close();
        }
    }

    class TestThread extends Thread {
        Exception e = null;
        CountDownLatch lacth;

        TestThread(CountDownLatch lacth) {
            this.lacth = lacth;
        }

        public void run() {
            ObjectBaseHandle proxy = null;
            try {
                proxy = (ObjectBaseHandle) obs.getObjectHandle();
            } catch (Exception e) {
                this.e = e;
            } finally {
                if (proxy != null)
                    try {
                        proxy.close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
            }
            lacth.countDown();
        }
    }
}
