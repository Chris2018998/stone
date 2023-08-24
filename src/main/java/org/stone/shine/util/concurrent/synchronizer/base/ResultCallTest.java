/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.base;

import org.stone.shine.util.concurrent.synchronizer.SyncNode;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;

/**
 * tester before executing call
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ResultCallTest {

    public static final ResultCallTest Tester = new ResultCallTest();

    ResultCallTest() {
    }

    //true,result call will be execute in pool
    public boolean canCall(boolean fair, SyncNode firstNode, SyncVisitConfig config) {
        return firstNode == null || !fair;
    }
}
