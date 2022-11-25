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

import org.stone.shine.concurrent.linkedTransferQueue.threads.PollThread;
import org.stone.test.TestUtil;

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TryTransferSuccessTest extends BaseTestCase {

    public void test() throws Exception {
        PollThread mockThread = new PollThread(queue, "take");
        mockThread.start();

        LockSupport.parkNanos(ParkDelayNanos);
        Object transferObj = new Object();
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.tryTransfer(transferObj));
        LockSupport.parkNanos(ParkDelayNanos);
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", transferObj, mockThread.getResult());
    }
}