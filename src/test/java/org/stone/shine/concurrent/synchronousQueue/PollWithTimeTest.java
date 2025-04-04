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
import org.stone.shine.concurrent.synchronousQueue.threads.PollThread;
import org.stone.shine.util.concurrent.SynchronousQueue;

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * synchronousQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PollWithTimeTest extends TestCase {

    public void test() throws Exception {
        SynchronousQueue queue = new SynchronousQueue(true);

        //2:create one mock Thread
        PollThread mockThread = new PollThread(queue, "poll", Wait_Time, Wait_TimeUnit);
        mockThread.start();


        Object offerObject = new Object();
        if (TestUtil.waitUtilWaiting(mockThread)) ;
        queue.offer(offerObject);

        //5:poll object from queue
        mockThread.join();
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", offerObject, mockThread.getResult());
    }
}
