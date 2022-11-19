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
import org.stone.shine.countDownLatch.threads.GeneralAwaitThread;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * CountDownLatch Test Case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AwaitTimeOutTest extends TestCase {
    public void test() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        //1:create a wait thread
        long timeout = 5L;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        GeneralAwaitThread waitThread = new GeneralAwaitThread(latch, timeout, timeUnit);
        waitThread.start();

        //2:park main thread 5 seconds
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(6));

        //3:check timeout == true
        if (latch.getCount() == 0) TestUtil.assertError("Await timeout test failed ");
    }
}
