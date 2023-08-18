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
import org.stone.shine.concurrent.ConcurrentTimeUtil;
import org.stone.shine.concurrent.synchronousQueue.threads.PollThread;
import org.stone.shine.util.concurrent.SynchronousQueue;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkNanos;

/**
 * synchronousQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PollInterruptedTest extends TestCase {

    public void test() throws Exception {
        SynchronousQueue queue = new SynchronousQueue(true);

        //2:create one mock Thread
        PollThread mockThread = new PollThread(queue, "take");
        mockThread.start();

        //3:park main thread 1 seconds and check mock thread result
        if (ConcurrentTimeUtil.isInWaiting(mockThread, ParkNanos))
            mockThread.interrupt();

        mockThread.join();
        if (mockThread.getInterruptedException() == null)
            TestUtil.assertError("Test failed,put thread not be interrupted");
    }
}
