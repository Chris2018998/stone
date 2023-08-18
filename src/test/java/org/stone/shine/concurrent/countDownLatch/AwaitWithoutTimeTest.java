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
import org.stone.shine.concurrent.ConcurrentTimeUtil;
import org.stone.shine.concurrent.countDownLatch.threads.ZeroCountWaitThread;
import org.stone.shine.util.concurrent.CountDownLatch;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkNanos;

/**
 * CountDownLatch Test Case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AwaitWithoutTimeTest extends TestCase {
    public static void main(String[] args) throws Exception {
        AwaitWithoutTimeTest tester = new AwaitWithoutTimeTest();
        tester.test();
    }

    public void test() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        //1:create a wait thread
        ZeroCountWaitThread waitThread = new ZeroCountWaitThread(latch, "await");
        waitThread.start();

        //2: detect wait thread
        if (ConcurrentTimeUtil.isInWaiting(waitThread, ParkNanos))
            latch.countDown();

        //3: get timeout indicator from await thread
        waitThread.join();
    }
}
