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

import org.stone.util.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * node cas updater
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ThreadNodeUpdater {
    private final static Unsafe U;
    private final static long stateOffSet;

    //chain field offset
    private final static long prevOffSet;
    private final static long nextOffSet;
    private final static long emptyIndOffSet;

    static {
        try {
            U = UnsafeUtil.getUnsafe();
            Class nodeClass = ThreadNode.class;
            //ThreadNode.state
            Field stateField = nodeClass.getDeclaredField("state");
            stateField.setAccessible(true);
            stateOffSet = U.objectFieldOffset(stateField);

            //ThreadNode.prev
            Field prevField = nodeClass.getDeclaredField("prev");
            prevField.setAccessible(true);
            prevOffSet = U.objectFieldOffset(prevField);
            //ThreadNode.next
            Field nextField = nodeClass.getDeclaredField("next");
            nextField.setAccessible(true);
            nextOffSet = U.objectFieldOffset(nextField);
            //ThreadNode.emptyInd
            Field emptyIndField = nodeClass.getDeclaredField("emptyInd");
            emptyIndField.setAccessible(true);
            emptyIndOffSet = U.objectFieldOffset(emptyIndField);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static boolean casNodeState(ThreadNode node, Object expect, Object update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    static boolean casTailNext(ThreadNode t, ThreadNode newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    static boolean logicRemove(ThreadNode node) {
        return node.getEmptyInd() == 0 && U.compareAndSwapInt(node, emptyIndOffSet, 0, 1);
    }

    static void unlinkFromChain(ThreadNode node) {
        ThreadNode prev = node.getPrev();
        ThreadNode next = node.getNext();
        if (prev != null && next != null) linkNextTo(prev, next);
    }

    private static void linkNextTo(ThreadNode startNode, ThreadNode endNode) {
        //startNode.next ----> endNode
        ThreadNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        ThreadNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }
}
