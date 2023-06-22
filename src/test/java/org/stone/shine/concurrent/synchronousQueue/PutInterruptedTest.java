/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.synchronousQueue;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.shine.util.concurrent.SynchronousQueue;
import org.stone.shine.concurrent.synchronousQueue.threads.OfferThread;

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * synchronousQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PutInterruptedTest extends TestCase {

    public void test() throws Exception {
        SynchronousQueue queue = new SynchronousQueue(true);

        //2:create one mock Thread
        OfferThread mockThread = new OfferThread(queue, "put", new Object());
        mockThread.start();

        //3:park main thread 3 seconds and check mock thread result
        LockSupport.parkNanos(ParkDelayNanos);
        if (mockThread.getState() != Thread.State.WAITING)
            TestUtil.assertError("Test failed,put thread not in waiting");

        //4:interrupt the mock thread
        mockThread.interrupt();
        LockSupport.parkNanos(ParkDelayNanos);
        if (mockThread.getInterruptedException() == null)
            TestUtil.assertError("Test failed,put thread not be interrupted");
    }
}
