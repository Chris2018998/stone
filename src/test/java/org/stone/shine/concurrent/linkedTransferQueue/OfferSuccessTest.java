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

public class OfferSuccessTest extends BaseTestCase {

    public void test() throws Exception {
        PollThread mockThread = new PollThread(queue, "take");
        mockThread.start();

        Object offerObj = new Object();
        if (TestUtil.waitUtilWaiting(mockThread)) {
            TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.offer(offerObj));
        }

        mockThread.join();
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", offerObj, mockThread.getResult());
    }
}
