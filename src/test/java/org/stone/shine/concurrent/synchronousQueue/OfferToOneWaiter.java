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

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * synchronousQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class OfferToOneWaiter extends TestCase {

    public void test() throws Exception {
        SynchronousQueue<Object> queue = new SynchronousQueue<>(true);

        //1: create a poll thread to take transferred object
        PollThread pollThread = new PollThread(queue, "take");
        pollThread.start();

        //2: try transfer object to waiter(poll thread)
        LockSupport.parkNanos(ParkDelayNanos);
        Object offerObject = new Object();
        boolean offerResult = queue.offer(offerObject);
        LockSupport.parkNanos(ParkDelayNanos);

        //3: check result
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, offerResult);
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", offerObject, pollThread.getResult());
    }
}
