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

import org.stone.shine.concurrent.Semaphore;
import org.stone.shine.concurrent.semaphore.threads.AcquireMockThread;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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

        //2:create one mock Thread
        AcquireMockThread mockThread = new AcquireMockThread(semaphore, "acquire");
        mockThread.start();

        //3:park main thread 1 second
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));

        //4:mock interrupt
        TestUtil.assertError("test fail,expect value:%s,actual value:%s", true, semaphore.hasQueuedThreads());
        TestUtil.assertError("test fail,expect value:%s,actual value:%s", 1, semaphore.getQueueLength());

        //5: semaphore release from main thread
        semaphore.release();
    }

}
