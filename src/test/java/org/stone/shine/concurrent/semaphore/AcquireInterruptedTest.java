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
import org.stone.shine.concurrent.ConcurrentTimeUtil;
import org.stone.shine.concurrent.semaphore.threads.AcquireMockThread;
import org.stone.shine.util.concurrent.Semaphore;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkNanos;

/**
 * Semaphore Test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AcquireInterruptedTest extends TestCase {

    public void test() throws Exception {
        Semaphore semaphore = new Semaphore(1);

        //1:take out the only one permit by main thread
        semaphore.acquire();

        //2:create one mock Thread
        AcquireMockThread mockThread = new AcquireMockThread(semaphore, "acquire");
        mockThread.start();

        //3:park main thread 5 seconds and check mock thread state
        if (ConcurrentTimeUtil.isInWaiting(mockThread, ParkNanos)) {
            mockThread.interrupt();
            return;
        }

        //4: semaphore release from main thread
        semaphore.release();
    }
}
