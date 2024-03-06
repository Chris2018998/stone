/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer;

import org.stone.shine.util.concurrent.synchronizer.chain.SyncNode;
import org.stone.tools.CommonUtil;

/**
 * @author Chris Liao
 * @version 1.0
 */

public final class SyncConstants {

    //Integer 1
    public static final Integer INT_ONE = 1;

    //Sharable acquisition type
    public static final Object TYPE_SHARED = new Object();

    //Exclusive acquisition type
    public static final Object TYPE_EXCLUSIVE = new Object();

    //base visit tester
    public static final SyncVisitTester BASE_VISIT_TESTER = new SyncVisitTester() {
        public final boolean allow(boolean unfair, Object curType, ObjectWaitPool pool) {
            return unfair || pool.peekFirst() == null;
        }
    };

    //share visit tester
    public static final SyncVisitTester SHARE_VISIT_TESTER = new SyncVisitTester() {
        public final boolean allow(boolean unfair, Object curType, ObjectWaitPool pool) {
            SyncNode first = pool.peekFirst();
            return first == null || unfair && CommonUtil.objectEquals(first.getType(), curType);
        }
    };
}
