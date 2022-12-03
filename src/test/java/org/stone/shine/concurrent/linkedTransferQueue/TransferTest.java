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
import org.stone.shine.concurrent.linkedTransferQueue.threads.TransferThread;

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_TimeoutNanos;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TransferTest extends BaseTestCase {

    public void test() throws Exception {
        Object transferObj = new Object();
        TransferThread mockThread = new TransferThread(queue, "transfer", transferObj);
        mockThread.start();

        LockSupport.parkNanos(Global_TimeoutNanos);
        if (mockThread.getState() != Thread.State.WAITING) TestUtil.assertError("Test failed");

        Object transferObj2 = queue.poll();
        TestUtil.assertError("test failed expect value:%s,actual value:%s", transferObj, transferObj2);
    }
}