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
import org.stone.shine.concurrent.countDownLatch.threads.GeneralAwaitThread;
import org.stone.shine.util.concurrent.CountDownLatch;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_TimeUnit;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_Timeout;

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
        GeneralAwaitThread waitThread = new GeneralAwaitThread(latch, "await", Global_Timeout, Global_TimeUnit);
        waitThread.setOwnerCase(this);
        waitThread.start();

        //3:check timeout == true
        waitThread.join();
        if (latch.getCount() == 0) TestUtil.assertError("Await timeout test failed ");
    }
}
