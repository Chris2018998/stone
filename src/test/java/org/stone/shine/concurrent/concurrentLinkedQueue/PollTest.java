/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.concurrentLinkedQueue;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.shine.util.concurrent.ConcurrentLinkedQueue;

/**
 * concurrentLinkedQueue test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PollTest extends TestCase {

    public void test() throws Exception {
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();

        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
        queue.offer(o1);
        queue.offer(o2);
        queue.offer(o3);

        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", o1, queue.poll());
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", o2, queue.poll());
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", o3, queue.poll());
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", 0, queue.size());
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", null, queue.peek());
    }
}
