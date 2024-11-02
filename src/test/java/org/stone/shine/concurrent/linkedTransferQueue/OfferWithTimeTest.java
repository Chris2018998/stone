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
import org.stone.shine.concurrent.linkedTransferQueue.threads.OfferThread;

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class OfferWithTimeTest extends BaseTestCase {

    public static void main(String[] args) throws Exception {
        OfferWithTimeTest tester = new OfferWithTimeTest();
        tester.setUp();
        tester.test();
    }

    public void test() throws Exception {
        //1:create one mock Thread
        Object object = new Object();
        OfferThread mockThread = new OfferThread(queue, "offer", object, Wait_Time, Wait_TimeUnit);
        mockThread.start();

        Object object2 = null;
        if (TestUtil.waitUtilWaiting(mockThread))
            object2 = queue.poll();

        //2:park main thread
        mockThread.join();
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", object, object2);
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, mockThread.getResult());
    }
}