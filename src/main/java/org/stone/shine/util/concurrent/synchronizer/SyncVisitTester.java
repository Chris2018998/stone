/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

import org.stone.shine.util.concurrent.synchronizer.chain.SyncNode;
import org.stone.tools.CommonUtil;

/**
 * synchronization tester on first Visit
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface SyncVisitTester {
    SyncVisitTester BASE_VISIT_TESTER = new SyncVisitTester() {
        public final boolean allow(boolean fair, Object curType, BaseWaitPool pool) {
            return !fair || pool.peekFirst() == null;
        }
    };
    //avoid starvation
    SyncVisitTester SHARE_VISIT_TESTER = new SyncVisitTester() {
        public final boolean allow(boolean fair, Object curType, BaseWaitPool pool) {
            SyncNode first = pool.peekFirst();
            return first == null || !fair && CommonUtil.objectEquals(first.getType(), curType);
        }
    };

    //interface method
    boolean allow(boolean fair, Object curType, BaseWaitPool pool);
}
