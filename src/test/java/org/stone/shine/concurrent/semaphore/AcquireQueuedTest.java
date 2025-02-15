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

/**
 * Semaphore Test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AcquireQueuedTest extends TestCase {

    public void test() throws Exception {
        Semaphore semaphore = new Semaphore(1);

        //1:take out the only one permit by main thread
        semaphore.acquire();
        TestUtil.assertError("test fail,expect value:%s,actual value:%s", 0, semaphore.availablePermits());

        //2:create one mock Thread
        AcquireMockThread mockThread = new AcquireMockThread(semaphore, "acquire");
        mockThread.start();

        //3:park main thread 2 second
        if (TestUtil.waitUtilWaiting(mockThread)) {
            //4:mock interrupt
            TestUtil.assertError("test fail,expect value:%s,actual value:%s", true, semaphore.hasQueuedThreads());
            TestUtil.assertError("test fail,expect value:%s,actual value:%s", 1, semaphore.getQueueLength());
            mockThread.interrupt();
        }

        //5: semaphore release from main thread
        mockThread.join();
        semaphore.release();
    }

}
