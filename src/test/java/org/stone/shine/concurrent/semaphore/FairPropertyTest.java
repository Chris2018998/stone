/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.semaphore;

import org.stone.shine.concurrent.Semaphore;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

/**
 * Semaphore Test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class FairPropertyTest extends TestCase {

    public void test() throws Exception {
        Semaphore semaphore1 = new Semaphore(1);
        TestUtil.assertError("fair test expect value:%s,actual value:%s", false, semaphore1.isFair());

        Semaphore semaphore2 = new Semaphore(1, true);
        TestUtil.assertError("fair test expect value:%s,actual value:%s", true, semaphore2.isFair());
    }
}
