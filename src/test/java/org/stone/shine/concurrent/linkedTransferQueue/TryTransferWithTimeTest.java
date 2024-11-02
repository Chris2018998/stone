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

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TryTransferWithTimeTest extends BaseTestCase {

    public static void main(String[] args) throws Exception {
        TryTransferWithTimeTest tester = new TryTransferWithTimeTest();
        tester.setUp();
        tester.test();
    }

    public void test() throws Exception {
        Object transferObj = new Object();
        TransferThread mockThread = new TransferThread(queue, "tryTransfer", transferObj, Wait_Time, Wait_TimeUnit);
        mockThread.start();

        Object transferObj2 = null;
        if (TestUtil.waitUtilWaiting(mockThread))
            transferObj2 = queue.poll();

        mockThread.join();
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", transferObj, transferObj2);
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, mockThread.getResult());
    }
}