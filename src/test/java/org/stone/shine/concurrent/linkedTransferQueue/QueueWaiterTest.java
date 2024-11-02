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

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class QueueWaiterTest extends BaseTestCase {

    public static void main(String[] args) throws Exception {
        QueueWaiterTest tester = new QueueWaiterTest();
        tester.setUp();
        tester.test();
    }

    public void test() throws Exception {
        PollThread mockThread1 = new PollThread(queue, "take");
        mockThread1.start();

        PollThread mockThread2 = new PollThread(queue, "take");
        mockThread2.start();

        TestUtil.waitUtilWaiting(mockThread1);
        TestUtil.waitUtilWaiting(mockThread2);
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.hasWaitingConsumer());
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", 2, queue.getWaitingConsumerCount());
        mockThread1.interrupt();
        mockThread2.interrupt();
    }
}
