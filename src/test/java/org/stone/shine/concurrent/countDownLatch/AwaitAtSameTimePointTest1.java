/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.countDownLatch;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.shine.concurrent.ConcurrentTimeUtil;
import org.stone.shine.concurrent.countDownLatch.threads.CountDownThread;
import org.stone.shine.concurrent.countDownLatch.threads.SameTimePointToAwaitThread;
import org.stone.shine.util.concurrent.CountDownLatch;

import java.util.concurrent.locks.LockSupport;

/**
 * await without time test case:(all thread wait at same time point:concurrent mock)
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AwaitAtSameTimePointTest1 extends TestCase {

    public void test() throws Exception {
        int count = 100;
        CountDownLatch latch = new CountDownLatch(count);

        //1:create count down threads
        CountDownThread[] countDownThreads = new CountDownThread[count];
        for (int i = 0; i < count; i++) countDownThreads[i] = new CountDownThread(latch);

        //2:cal the expect time point value to wait(2 seconds)
        long timePointToAWait = ConcurrentTimeUtil.getConcurrentNanoSeconds(2);

        //3:create countWait threads and park them
        SameTimePointToAwaitThread[] sameTimePointWaitThreads = new SameTimePointToAwaitThread[count];
        for (int i = 0; i < count; i++) {
            sameTimePointWaitThreads[i] = new SameTimePointToAwaitThread(latch, timePointToAWait, "await");
            sameTimePointWaitThreads[i].start();
        }

        //4: crate count down threads
        LockSupport.parkNanos(timePointToAWait - System.nanoTime());
        for (int i = 0; i < count; i++) countDownThreads[i].start();

        //5:park main thread
        latch.await();

        //6:count value should be zero
        TestUtil.assertError("final count value expect value:%s,actual value:%s", 0, (int) latch.getCount());
    }
}
