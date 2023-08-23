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

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PollWithTimeTest extends BaseTestCase {

    public void test() throws Exception {
        //2:create one mock Thread
        PollThread mockThread = new PollThread(queue, "poll", Wait_Time, Wait_TimeUnit);
        mockThread.start();

        //3:park main thread 1 seconds and check mock thread result
        if (!TestUtil.joinUtilWaiting(mockThread))
            TestUtil.assertError("Test failed,put thread not in waiting");

        //4:poll object from queue
        Object offerObject = new Object();
        queue.offer(offerObject);

        //5:poll object from queue
        mockThread.join();
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", offerObject, mockThread.getResult());
    }
}
