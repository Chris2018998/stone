/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer;

import org.stone.util.atomic.UnsafeAdaptor;
import org.stone.util.atomic.UnsafeAdaptorHolder;

/**
 * node cas updater
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class CasNodeUpdater {
    private final static UnsafeAdaptor U;
    private final static long stateOffSet;

    //chain field offset
    private final static long tailOffSet;
    private final static long prevOffSet;
    private final static long nextOffSet;

    static {
        try {
            U = UnsafeAdaptorHolder.U;
            Class nodeClass = CasNode.class;
            //ThreadNode.state
            stateOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("state"));
            //ThreadNode.prev
            prevOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("prev"));
            //ThreadNode.next
            nextOffSet = U.objectFieldOffset(nodeClass.getDeclaredField("next"));
            //CasNodeChain.tail
            tailOffSet = U.objectFieldOffset(CasNodeChain.class.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static boolean casTail(CasNodeChain chain, CasNode expect, CasNode update) {
        return U.compareAndSwapObject(chain, tailOffSet, expect, update);
    }

    public static boolean casPrev(CasNode n, CasNode curPrev, CasNode newPrev) {
        return U.compareAndSwapObject(n, prevOffSet, curPrev, newPrev);
    }

    public static boolean casNext(CasNode n, CasNode curNext, CasNode newNext) {
        return U.compareAndSwapObject(n, nextOffSet, curNext, newNext);
    }

    public static boolean casState(CasNode node, Object expect, Object update) {
        return node.state == expect && U.compareAndSwapObject(node, stateOffSet, expect, update);
    }
}
