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

import org.stone.shine.concurrent.SynchronousQueue;
import org.stone.shine.concurrent.synchronousQueue.threads.PollThread;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * synchronousQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PollWithTimeTest extends TestCase {

    public void test() throws Exception {
        SynchronousQueue queue = new SynchronousQueue(true);

        //2:create one mock Thread
        PollThread mockThread = new PollThread(queue, "poll", 3, TimeUnit.SECONDS);
        mockThread.start();

        //3:park main thread 1 seconds and check mock thread result
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        if (mockThread.getState() != Thread.State.TIMED_WAITING)
            TestUtil.assertError("Test failed,put thread not in waiting");

        //4:poll object from queue
        Object offerObject = new Object();
        queue.offer(offerObject);

        //5:poll object from queue
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", offerObject, mockThread.getResult());
    }
}
