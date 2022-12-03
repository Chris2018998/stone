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

import org.stone.base.TestCase;
import org.stone.shine.concurrent.LinkedTransferQueue;

/**
 * LinkedTransferQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */
class BaseTestCase extends TestCase {

    protected LinkedTransferQueue queue;

    public void setUp() {
        this.queue = new LinkedTransferQueue();
    }
}
