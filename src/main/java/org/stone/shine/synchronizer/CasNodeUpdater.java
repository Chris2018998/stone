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

import static org.stone.shine.synchronizer.CasStaticState.REMOVED;

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
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    //****************************************************************************************************************//
    //                                 1: remove data node state with cas way(used outside of synchronizer pckage)    //
    //****************************************************************************************************************//
    public static boolean logicRemove(CasNode node) {
        Object state = node.getState();
        return state != REMOVED && U.compareAndSwapObject(node, stateOffSet, state, REMOVED);
    }

    //****************************************************************************************************************//
    //                                        2: update syn node state with cas way                                   //
    //****************************************************************************************************************//
    public static boolean casNodeState(CasNode node, Object expect, Object update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    //****************************************************************************************************************//
    //                                                3: Cas Chain method(4)                                          //
    //****************************************************************************************************************//
    public static boolean casPrev(CasNode n, CasNode curPrev, CasNode newPrev) {
        return U.compareAndSwapObject(n, prevOffSet, curPrev, newPrev);
    }

    public static boolean casNext(CasNode n, CasNode curNext, CasNode newNext) {
        return U.compareAndSwapObject(n, nextOffSet, curNext, newNext);
    }

    static void unlinkFromChain(CasNode node) {
        CasNode prev = node.getPrev();
        CasNode next = node.getNext();
        if (prev != null && next != null) linkNextTo(prev, next);
    }

    private static void linkNextTo(CasNode startNode, CasNode endNode) {
        //startNode.next ----> endNode
        CasNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        CasNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }
}
