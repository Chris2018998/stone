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

import java.util.ArrayList;
import java.util.List;

/**
 * concurrentLinkedQueue test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class RetainAllTest extends TestCase {

    public void test() throws Exception {
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
        List subList = new ArrayList();
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();
        Object o4 = new Object();
        Object o5 = new Object();
        Object o6 = new Object();
        Object o7 = new Object();

        subList.add(o3);
        subList.add(o4);
        subList.add(o7);

        queue.offer(o1);
        queue.offer(o2);
        queue.offer(o3);
        queue.offer(o4);
        queue.offer(o5);
        queue.offer(o6);
        queue.offer(o7);

        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(o3));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(o4));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(o7));
        queue.retainAll(subList);
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(o3));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(o4));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", true, queue.contains(o7));

        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, queue.contains(o1));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, queue.contains(o2));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, queue.contains(o5));
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, queue.contains(o6));
    }
}
