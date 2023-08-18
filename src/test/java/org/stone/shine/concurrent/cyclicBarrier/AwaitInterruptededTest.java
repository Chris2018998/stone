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
import org.stone.shine.concurrent.ConcurrentTimeUtil;
import org.stone.shine.concurrent.cyclicBarrier.threads.BarrierAwaitThread;
import org.stone.shine.util.concurrent.CyclicBarrier;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkNanos;

/**
 * CyclicBarrier Test Case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AwaitInterruptededTest extends TestCase {
    public static void main(String[] args) throws Exception {
        AwaitInterruptededTest tester = new AwaitInterruptededTest();
        tester.test();
    }

    public void test() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);

        //1:create a wait thread
        BarrierAwaitThread waitThread = new BarrierAwaitThread(barrier, "await");
        waitThread.start();

        //2: detect wait thread
        if (ConcurrentTimeUtil.isInWaiting(waitThread, ParkNanos))
            waitThread.interrupt();

        //3: check InterruptedException
        waitThread.join();
        InterruptedException e = waitThread.getInterruptedException();
        if (e == null) TestUtil.assertError("Await Interrupteded Test failed ");
    }
}
