/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.linkedTransferQueue;

import org.stone.base.TestUtil;
import org.stone.shine.concurrent.linkedTransferQueue.threads.PollThread;

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class QueueWaiterTest extends BaseTestCase {

    public void test() throws Exception {
        PollThread mockThread1 = new PollThread(queue, "take");
        mockThread1.start();
        PollThread mockThread2 = new PollThread(queue, "take");
        mockThread2.start();

        LockSupport.parkNanos(ParkDelayNanos);
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.hasWaitingConsumer());
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", 2, queue.getWaitingConsumerCount());
    }
}
