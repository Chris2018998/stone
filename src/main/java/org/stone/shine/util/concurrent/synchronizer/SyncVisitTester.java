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

import org.stone.tools.CommonUtil;

/**
 * synchronization tester on first Visit
 *
 * @author Chris Liao
 * @version 1.0
 */

public class SyncVisitTester {
    public static final SyncVisitTester BASE_VISIT_TESTER = new SyncVisitTester();

    public static final SyncVisitTester SHARE_VISIT_TESTER = new ShareVisitTester();

    private SyncVisitTester() {
    }

    public boolean test(boolean fair, SyncNode firstNode, SyncVisitConfig config) {
        return firstNode == null || !fair;
    }

    //avoid starvation
    private static final class ShareVisitTester extends SyncVisitTester {
        public final boolean test(boolean fair, SyncNode firstNode, SyncVisitConfig config) {
            return firstNode == null || !fair && CommonUtil.objectEquals(firstNode.getType(), config.getNodeType());
        }
    }
}
