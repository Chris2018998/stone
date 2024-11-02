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
import org.stone.shine.concurrent.countDownLatch.threads.ZeroCountWaitThread;
import org.stone.shine.util.concurrent.CountDownLatch;

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * CountDownLatch Test Case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AwaitWithTimeTest extends TestCase {
    public static void main(String[] args) throws Exception {
        AwaitWithTimeTest tester = new AwaitWithTimeTest();
        tester.test();
    }

    public void test() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        //1:create a wait thread
        ZeroCountWaitThread waitThread = new ZeroCountWaitThread(latch, "await", Wait_Time, Wait_TimeUnit);
        waitThread.start();

        //2: detect wait thread
        if (TestUtil.waitUtilWaiting(waitThread))
            latch.countDown();

        //3:get timeout indicator from await thread
        waitThread.join();
    }
}
