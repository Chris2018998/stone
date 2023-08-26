/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998(cn),All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

import org.stone.tools.CommonUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.REMOVED;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.objectEquals;

/**
 * Base Wait-Wakeup Pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class ThreadWaitingPool {
    //private final SyncNodeChain waitChain = new SyncNodeChain();
    private final ConcurrentLinkedDeque<SyncNode> waitChain = new ConcurrentLinkedDeque<>();//temporary

    //****************************************************************************************************************//
    //                                          1:queue Methods(4)                                                    //
    //****************************************************************************************************************//
    protected final SyncNode firstNode() {
        return waitChain.peekFirst();
    }

    protected final boolean atFirst(SyncNode node) {
        return node == waitChain.peekFirst();
    }

    protected final void removeNode(SyncNode node) {
        waitChain.removeFirstOccurrence(node);
    }

    protected final void appendAsDataNode(SyncNode node) {
        node.state = null;
        node.thread = null;
        waitChain.offerLast(node);
    }

    protected final boolean appendAsWaitNode(SyncNode node) {
        node.setCurrentThread();
        waitChain.offerLast(node);
        return node == waitChain.peekFirst();
    }

    //****************************************************************************************************************//
    //                                          2: wakeup(3)                                                          //
    //****************************************************************************************************************//
    public final void wakeupFirst(Object nodeType, Object toState) {//use in result wait pool
        Iterator<SyncNode> iterator = waitChain.iterator();

        //find a valid node as first
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode first = iterator.next();//assume it is first node
                if (first.getState() == REMOVED) continue;
                if (casState(first, null, toState))
                    LockSupport.unpark(first.thread);
                return;
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode first = iterator.next();//assume it is first node
                if (first.getState() == REMOVED) continue;
                if (!CommonUtil.objectEquals(nodeType, first.type))
                    return;
                if (casState(first, null, toState))
                    LockSupport.unpark(first.thread);
                return;
            }
        }
    }

    public final SyncNode wakeupOne(boolean fromHead, Object nodeType, Object toState) {
        Iterator<SyncNode> iterator = fromHead ? waitChain.iterator() : waitChain.descendingIterator();

        //2: retrieve type matched node and unpark its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                    return qNode;
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if ((nodeType == qNode.type || nodeType.equals(qNode.type)) && casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                    return qNode;
                }
            }
        }
        //3: not found matched node
        return null;
    }

    public final void wakeupAll(boolean fromHead, Object nodeType, Object toState) {
        Iterator<SyncNode> iterator = fromHead ? waitChain.iterator() : waitChain.descendingIterator();

        //2: retrieve type matched node and unpark its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                Object state = qNode.getState();
                if (state != REMOVED) {
                    if (casState(qNode, null, toState))
                        LockSupport.unpark(qNode.thread);
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                Object state = qNode.getState();
                if (state != REMOVED) {
                    if ((nodeType == qNode.type || nodeType.equals(qNode.type)) && casState(qNode, null, toState)) {
                        LockSupport.unpark(qNode.thread);
                    }
                }
            }
        }
    }

    //****************************************************************************************************************//
    //                                         3: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
    protected final Iterator<SyncNode> ascendingIterator() {
        return waitChain.iterator();
    }

    protected final boolean existsTypeNode(Object nodeType) {
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (objectEquals(nodeType, node.type)) return true;
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null && node.thread != null) return true;
        }
        return false;
    }

    public final boolean hasQueuedThread(Thread thread) {
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.thread == thread) return true;
        }
        return false;
    }

    public final int getQueueLength() {
        int count = 0;
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null) count++;
        }
        return count;
    }

    public final Collection<Thread> getQueuedThreads() {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null && node.thread != null) threadList.add(node.thread);
        }
        return threadList;
    }

    protected int getQueueLength(Object nodeType) {
        if (nodeType == null) return getQueueLength();

        int count = 0;
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (objectEquals(nodeType, node.type) && node.state == null) count++;
        }
        return count;
    }

    protected Collection<Thread> getQueuedThreads(Object nodeType) {
        if (nodeType == null) return getQueuedThreads();

        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (objectEquals(nodeType, node.type) && node.state == null && node.thread != null)
                threadList.add(node.thread);
        }
        return threadList;
    }
}
