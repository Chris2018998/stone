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

import org.stone.test.TestUtil;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class OfferFailTest extends BaseTestCase {

    public void test() throws Exception {
        Object offerObj = new Object();
        boolean resultInd = queue.offer(offerObj);

        Object offerObj2 = queue.peek();
        Object offerObj3 = queue.poll();

        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", false, resultInd);
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", offerObj, offerObj2);
        TestUtil.assertError("Test failed,expect value:%s,actual value:%s", offerObj, offerObj3);
    }
}

