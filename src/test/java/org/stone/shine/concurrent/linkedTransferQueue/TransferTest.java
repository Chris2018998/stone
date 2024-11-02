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

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TransferTest extends BaseTestCase {

    public static void main(String[] args) throws Exception {
        TransferTest tester = new TransferTest();
        tester.setUp();
        tester.test();
    }


    public void test() throws Exception {
        Object transferObj = new Object();
        TransferThread mockThread = new TransferThread(queue, "transfer", transferObj);
        mockThread.start();

        //2: detect wait thread
        Object transferObj2 = null;
        if (TestUtil.waitUtilWaiting(mockThread))
            transferObj2 = queue.poll();

        mockThread.join();
        TestUtil.assertError("test failed expect value:%s,actual value:%s", transferObj, transferObj2);
    }
}