/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.countDownLatch;

import org.stone.shine.concurrent.CountDownLatch;
import org.stone.shine.countDownLatch.runnable.CountDownRunnable;
import org.stone.shine.countDownLatch.runnable.CountTimeWaitRunnable;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch Test Case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AwaitTimeTest extends TestCase {

    public void test() throws Exception {
        int count = 100;
        CountDownLatch latch = new CountDownLatch(count);

        //1: create countdown Threads
        CountDownRunnable[] runs = new CountDownRunnable[count];
        for (int i = 0; i < count; i++) runs[i] = new CountDownRunnable(latch);
        Thread[] countThreads = new Thread[count];
        for (int i = 0; i < count; i++) countThreads[i] = new Thread(runs[i]);

        //2:create wait Threads
        long timeout = 10l;
        TimeUnit unit = TimeUnit.SECONDS;

        Thread[] waitThreads = new Thread[count];
        for (int i = 0; i < count; i++) waitThreads[i] = new Thread(new CountTimeWaitRunnable(latch, timeout, unit));

        //4;run count down
        for (int i = 0; i < count; i++) countThreads[i].start();

        latch.await();//block main thread

        //count value should be zero
        TestUtil.assertError("final count value expect value:%s,actual value:%s", 0, (int) latch.getCount());
    }
}
