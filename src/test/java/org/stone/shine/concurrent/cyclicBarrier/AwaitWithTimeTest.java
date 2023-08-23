/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.cyclicBarrier;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.shine.concurrent.cyclicBarrier.threads.BarrierAwaitThread;
import org.stone.shine.util.concurrent.CyclicBarrier;

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * CyclicBarrier Test Case
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
        CyclicBarrier barrier = new CyclicBarrier(1);

        //1:create a wait thread
        BarrierAwaitThread waitThread = new BarrierAwaitThread(barrier, "await", Wait_Time, Wait_TimeUnit);
        waitThread.start();

        if (TestUtil.joinUtilWaiting(waitThread))
            barrier.await();

        //3:get timeout indicator from await thread
        waitThread.join();
        if (!new Integer(1).equals(waitThread.getResult())) TestUtil.assertError("Await with time test failed ");
    }
}
