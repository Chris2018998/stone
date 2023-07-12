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
import org.stone.shine.concurrent.semaphore.threads.AcquireMockThread;
import org.stone.shine.util.concurrent.Semaphore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_TimeUnit;

/**
 * Semaphore Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TryAcquireTimeOutTest extends TestCase {

    public void test() throws Exception {
        Semaphore semaphore = new Semaphore(1);

        //1:take out the only one permit by main thread
        semaphore.acquire();

        //2:create one mock Thread
        AcquireMockThread mockThread = new AcquireMockThread(semaphore, "tryAcquire", 1, Global_TimeUnit);
        mockThread.start();

        //3:park main thread 3 seconds and check mock thread state
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        TestUtil.assertError("TryAcquire test expect value:%s,actual value:%s", false, mockThread.getResult());

        //4:semaphore release from main thread
        semaphore.release();
    }
}
