/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.linkedTransferQueue;

import org.stone.base.TestUtil;
import org.stone.shine.concurrent.linkedTransferQueue.threads.OfferThread;
import org.stone.shine.concurrent.linkedTransferQueue.threads.PollThread;

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PollOneTest extends BaseTestCase {

    public void test() throws Exception {
        //1: create a offer thread
        Object offerObject = new Object();
        OfferThread offerThread = new OfferThread(queue, "put", offerObject);
        offerThread.start();

        //2: crate a poll thread
        LockSupport.parkNanos(ParkDelayNanos);
        PollThread pollThread = new PollThread(queue, "poll");
        pollThread.start();

        //3: check result
        LockSupport.parkNanos(ParkDelayNanos);
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", offerObject, pollThread.getResult());
    }
}
