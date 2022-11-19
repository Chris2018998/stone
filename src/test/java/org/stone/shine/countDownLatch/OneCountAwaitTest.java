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
import org.stone.shine.countDownLatch.threads.CountDownThread;
import org.stone.shine.countDownLatch.threads.GeneralAwaitThread;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

/**
 * 1001 Threads to await for one count value
 *
 * @author Chris Liao
 * @version 1.0
 */
public class OneCountAwaitTest extends TestCase {
    public void test() throws Exception {
        int count = 1;
        CountDownLatch latch = new CountDownLatch(count);

        //1:create count down threads
        CountDownThread[] countDownThreads = new CountDownThread[count];
        for (int i = 0; i < count; i++) countDownThreads[i] = new CountDownThread(latch);

        //2:create countWait threads and block them
        int waitCount = 1000;
        GeneralAwaitThread[] waitThreads = new GeneralAwaitThread[waitCount];
        for (int i = 0; i < waitCount; i++) {
            waitThreads[i] = new GeneralAwaitThread(latch);
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
