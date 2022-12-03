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
import org.stone.shine.concurrent.ConcurrentLinkedQueue;

import java.util.Iterator;

/**
 * concurrentLinkedQueue test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class IteratorRemoveTest extends TestCase {

    public void test() throws Exception {
        Object e1 = new Object();
        Object e2 = new Object();
        Object e3 = new Object();
        Object e4 = new Object();
        Object e5 = new Object();
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();

        queue.offer(e1);
        queue.offer(e2);
        queue.offer(e3);
        queue.offer(e4);
        queue.offer(e5);

        Iterator iterator = queue.iterator();
        while (iterator.hasNext()) {
            Object e = iterator.next();
            if (e == e2 || e == e4) {
                iterator.remove();
            }
        }

        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(e1));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, queue.contains(e2));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(e3));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, queue.contains(e4));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(e5));
    }
}
