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
import org.stone.util.atomic.UnsafeAdaptorFactory;

import java.lang.reflect.Field;

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
    private final static long prevOffSet;
    private final static long nextOffSet;

    static {
        try {
            U = UnsafeAdaptorFactory.get();
            Class nodeClass = CasNode.class;
            //ThreadNode.state
            Field stateField = nodeClass.getDeclaredField("state");
            if (!stateField.isAccessible()) stateField.setAccessible(true);
            stateOffSet = U.objectFieldOffset(stateField);

            //ThreadNode.prev
            Field prevField = nodeClass.getDeclaredField("prev");
            if (!prevField.isAccessible()) prevField.setAccessible(true);
            prevOffSet = U.objectFieldOffset(prevField);
            //ThreadNode.next
            Field nextField = nodeClass.getDeclaredField("next");
            if (!nextField.isAccessible()) nextField.setAccessible(true);
            nextOffSet = U.objectFieldOffset(nextField);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static boolean casPrev(CasNode n, CasNode curPrev, CasNode newPrev) {
        return U.compareAndSwapObject(n, prevOffSet, curPrev, newPrev);
    }

    public static boolean casNext(CasNode n, CasNode curNext, CasNode newNext) {
        return U.compareAndSwapObject(n, nextOffSet, curNext, newNext);
    }

    public static boolean casState(CasNode node, Object expect, Object update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }
}
