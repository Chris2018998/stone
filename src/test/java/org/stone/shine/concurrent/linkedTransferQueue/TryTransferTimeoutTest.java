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

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_TimeUnit;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_Timeout;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TryTransferTimeoutTest extends BaseTestCase {

    public void test() throws Exception {
        Object transferObj = new Object();
        TransferThread mockThread = new TransferThread(queue, "tryTransfer", transferObj, Global_Timeout, Global_TimeUnit);
        mockThread.start();

        mockThread.join();
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, mockThread.getResult());
    }
}