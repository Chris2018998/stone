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

import org.stone.tools.atomic.UnsafeAdaptor;
import org.stone.tools.atomic.UnsafeAdaptorHolder;

/**
 * node cas updater
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class SyncNodeUpdater {
    private final static UnsafeAdaptor U;
    private final static long stateOffSet;

    //chain field offset
    private final static long tailOffSet;
    private final static long prevOffSet;
    private final static long nextOffSet;

    static {
        try {
            U = UnsafeAdaptorHolder.U;
            Class nodeClass = SyncNode.class;
            //ThreadNode.state
            stateOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("state"));
            //ThreadNode.prev
            prevOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("prev"));
            //ThreadNode.next
            nextOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("next"));
            //SyncNodeChain.tail
            tailOffSet = U.objectFieldOffset(SyncNodeChain.class.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static boolean casTail(SyncNodeChain chain, SyncNode expect, SyncNode update) {
        return U.compareAndSwapObject(chain, tailOffSet, expect, update);
    }

    public static boolean casPrev(SyncNode n, SyncNode curPrev, SyncNode newPrev) {
        return U.compareAndSwapObject(n, prevOffSet, curPrev, newPrev);
    }

    public static boolean casNext(SyncNode n, SyncNode curNext, SyncNode newNext) {
        return U.compareAndSwapObject(n, nextOffSet, curNext, newNext);
    }

    public static boolean casState(SyncNode node, Object expect, Object update) {
        return node.state == expect && U.compareAndSwapObject(node, stateOffSet, expect, update);
    }
}