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

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * Semaphore Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class AcquireUninterruptedTest extends TestCase {

    public void test() throws Exception {
        Semaphore semaphore = new Semaphore(1);

        //1:take out the only one permit by main thread
        semaphore.acquire();

        //2:create one mock Thread
        AcquireMockThread mockThread = new AcquireMockThread(semaphore, "acquireUninterruptibly");
        mockThread.start();

        //3:park main thread 5 seconds and check mock thread state
        LockSupport.parkNanos(ParkDelayNanos);
        if (mockThread.getState() != Thread.State.WAITING) TestUtil.assertError("mock thread not in waiting");

        //3:mock interrupt
        mockThread.interrupt();
        LockSupport.parkNanos(ParkDelayNanos);
        if (mockThread.getState() != Thread.State.WAITING) TestUtil.assertError("mock thread not in waiting");

        //4: semaphore release from main thread
        semaphore.release();
    }
}

