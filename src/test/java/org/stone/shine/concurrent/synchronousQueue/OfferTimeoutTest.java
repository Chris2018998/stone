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

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.shine.concurrent.synchronousQueue.threads.OfferThread;
import org.stone.shine.util.concurrent.SynchronousQueue;

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * synchronousQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class OfferTimeoutTest extends TestCase {

    public void test() throws Exception {
        SynchronousQueue queue = new SynchronousQueue(true);

        //2:create one mock Thread
        OfferThread mockThread = new OfferThread(queue, "offer", new Object(), Wait_Time, Wait_TimeUnit);
        mockThread.start();

        //3:park main thread 3 seconds and check mock thread result
        mockThread.join();
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, mockThread.getResult());
    }
}
