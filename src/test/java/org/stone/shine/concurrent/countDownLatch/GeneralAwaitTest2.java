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
import org.stone.shine.concurrent.countDownLatch.threads.CountDownThread;
import org.stone.shine.concurrent.countDownLatch.threads.GeneralAwaitThread;
import org.stone.shine.util.concurrent.CountDownLatch;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_TimeUnit;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_Timeout;

/**
 * time await test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class GeneralAwaitTest2 extends TestCase {

    public void test() throws Exception {
        int count = 100;
        CountDownLatch latch = new CountDownLatch(count);

        //1:create count down threads
        CountDownThread[] countDownThreads = new CountDownThread[count];
        for (int i = 0; i < count; i++) countDownThreads[i] = new CountDownThread(latch);

        //2:create countWait threads and block them
        GeneralAwaitThread[] waitThreads = new GeneralAwaitThread[count];
        for (int i = 0; i < count; i++) {
            waitThreads[i] = new GeneralAwaitThread(latch, "await", Global_Timeout, Global_TimeUnit);
            waitThreads[i].start();
        }

        //3:crate count down threads
        for (int i = 0; i < count; i++) countDownThreads[i].start();

        //4:block main thread
        latch.await();

        //5:count value should be zero
        TestUtil.assertError("final count value expect value:%s,actual value:%s", 0, (int) latch.getCount());
    }
}
