/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.chain;

import org.stone.tools.unsafe.UnsafeAdaptorSunMiscImpl;
import sun.misc.Unsafe;

/**
 * node cas updater
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class SyncNodeUpdater {
    //chain field offset
    public final static long stateOffSet;
    private final static Unsafe U;
    private final static long tailOffSet;
    private final static long prevOffSet;
    private final static long nextOffSet;

    static {
        try {
            U = UnsafeAdaptorSunMiscImpl.U;
            Class nodeClass = SyncNode.class;
            //SyncNode.state
            stateOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("state"));
            //SyncNode.prev
            prevOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("prev"));
            //SyncNode.next
            nextOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("next"));
            //SyncNodeChain.tail
            tailOffSet = U.objectFieldOffset(SyncNodeChain.class.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static boolean casTail(SyncNodeChain chain, SyncNode expect, SyncNode update) {
        return chain.tail == expect && U.compareAndSwapObject(chain, tailOffSet, expect, update);
    }

    public static boolean casPrev(SyncNode n, SyncNode curPrev, SyncNode newPrev) {
        return n.prev == curPrev && U.compareAndSwapObject(n, prevOffSet, curPrev, newPrev);
    }

    public static boolean casNext(SyncNode n, SyncNode curNext, SyncNode newNext) {
        return n.next == curNext && U.compareAndSwapObject(n, nextOffSet, curNext, newNext);
    }

    public static boolean casState(SyncNode n, Object expect, Object update) {
        return n.state == expect && U.compareAndSwapObject(n, stateOffSet, expect, update);
    }

    public static boolean putState(SyncNode n, Object expect, Object update) {
        if (n.state == expect) {
            U.putOrderedObject(n, stateOffSet, update);
            return true;
        } else {
            return false;
        }
    }
}
