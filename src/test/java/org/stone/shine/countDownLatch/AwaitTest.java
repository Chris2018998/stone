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
import org.stone.shine.countDownLatch.runnable.CountWaitRunnable;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

import java.util.Objects;

/**
 * CountDownLatch Test Case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AwaitTest extends TestCase {

    public void test() throws Exception {
        int count = 100;
        CountDownLatch latch = new CountDownLatch(count);

        //1;create countdown Threads
        CountDownRunnable[] runs = new CountDownRunnable[count];
        for (int i = 0; i < count; i++) runs[i] = new CountDownRunnable(latch);
        Thread[] countThreads = new Thread[count];
        for (int i = 0; i < count; i++) countThreads[i] = new Thread(runs[i]);

        //2;create wait Threads
        Thread[] waitThreads = new Thread[count];
        for (int i = 0; i < count; i++) waitThreads[i] = new Thread(new CountWaitRunnable(latch));

        //3;run count down
        for (int i = 0; i < count; i++) countThreads[i].start();

        //4;run wait
        for (int i = 0; i < count; i++) waitThreads[i].start();

        latch.await();//block main thread

        //count value should be zero
        int latchCount = (int) latch.getCount();
        if (!Objects.equals(0, latchCount))
            TestUtil.assertError("password expect value:%s,actual value:%s", 0, latchCount);
    }
}
