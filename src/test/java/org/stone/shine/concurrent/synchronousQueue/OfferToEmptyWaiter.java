/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.synchronousQueue;

import org.stone.shine.concurrent.SynchronousQueue;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

/**
 * synchronousQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class OfferToEmptyWaiter extends TestCase {

    public void test() throws Exception {

        SynchronousQueue queue = new SynchronousQueue(true);

        TestUtil.assertError("test failed expect value:%s,actual value:%s", false, queue.offer(new Object()));
    }
}
