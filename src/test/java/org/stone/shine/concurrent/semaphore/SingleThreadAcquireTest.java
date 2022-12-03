/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.semaphore;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.shine.concurrent.Semaphore;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_TimeUnit;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_Timeout;

/**
 * Semaphore Test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SingleThreadAcquireTest extends TestCase {

    public void test() throws Exception {
        Semaphore semaphore = new Semaphore(1);

        //1:acquire test
        semaphore.acquire();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 0, semaphore.availablePermits());
        semaphore.release();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 1, semaphore.availablePermits());

        //2:acquireUninterruptibly test
        semaphore.acquireUninterruptibly();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 0, semaphore.availablePermits());
        semaphore.release();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 1, semaphore.availablePermits());

        //3:tryAcquire test
        semaphore.tryAcquire();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 0, semaphore.availablePermits());
        semaphore.release();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 1, semaphore.availablePermits());

        //4:tryAcquire(TimeUnit) test
        semaphore.tryAcquire(Global_Timeout, Global_TimeUnit);
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 0, semaphore.availablePermits());
        semaphore.release();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 1, semaphore.availablePermits());


        //5:acquire(permits) test
        semaphore.acquire(1);
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 0, semaphore.availablePermits());
        semaphore.release();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 1, semaphore.availablePermits());

        //6:acquireUninterruptibly(permits) test
        semaphore.acquireUninterruptibly(1);
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 0, semaphore.availablePermits());
        semaphore.release();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 1, semaphore.availablePermits());

        //7:tryAcquire(permits) test
        semaphore.tryAcquire(1);
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 0, semaphore.availablePermits());
        semaphore.release();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 1, semaphore.availablePermits());

        //8:tryAcquire(permits,TimeUnit) test
        semaphore.tryAcquire(1, Global_Timeout, Global_TimeUnit);
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 0, semaphore.availablePermits());
        semaphore.release();
        TestUtil.assertError("fair test expect value:%s,actual value:%s", 1, semaphore.availablePermits());
    }
}
