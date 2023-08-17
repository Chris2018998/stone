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
import org.stone.shine.concurrent.countDownLatch.threads.TimePointDownThread;
import org.stone.shine.concurrent.countDownLatch.threads.TimePointWaitThread;
import org.stone.shine.util.concurrent.CountDownLatch;

/**
 * time-await test case:(all thread wait at same time point:concurrent mock)
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConcurrentCountDownTest extends TestCase {

    public static void main(String[] args) throws Exception {
        ConcurrentCountDownTest tester = new ConcurrentCountDownTest();
        tester.test();
    }

    public void test() throws Exception {
        int count = 100;
        //1: create test latch
        CountDownLatch latch = new CountDownLatch(count);
        //2: create mock visit thread array
        TimePointDownThread[] countDownThreads = new TimePointDownThread[count];
        TimePointWaitThread[] zeroCountWaitThreads = new TimePointWaitThread[count];
        //3: compute time point for concurrent
        long concurrentTimePoint1 = System.nanoTime() + 5000L;

        //4: create threads and startup them
        for (int i = 0; i < count; i++) {
            zeroCountWaitThreads[i] = new TimePointWaitThread(latch, concurrentTimePoint1, "await");
            zeroCountWaitThreads[i].start();
        }

        //5: create threads and startup them
        long concurrentTimePoint2 = System.nanoTime() + 9000L;
        for (int i = 0; i < count; i++) {
            countDownThreads[i] = new TimePointDownThread(latch, concurrentTimePoint2);
            countDownThreads[i].start();
        }

        //6: block main thread
        latch.await();
        //7: count value should be zero
        TestUtil.assertError("final count value expect value:%s,actual value:%s", 0, latch.getCount());
    }
}
