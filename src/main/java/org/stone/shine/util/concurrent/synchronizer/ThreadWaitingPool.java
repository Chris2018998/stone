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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.REMOVED;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;
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
    private final Queue<SyncNode> waitChain = new ConcurrentLinkedQueue<>();//temporary

    //****************************************************************************************************************//
    //                                          1:queue Methods(5)                                                    //
    //****************************************************************************************************************//
    protected final SyncNode peekFirst() {
        return waitChain.peek();
    }

    protected final boolean atFirst(SyncNode node) {
        return waitChain.peek() == node;
    }

    protected final void removeNode(SyncNode node) {
        waitChain.remove(node);
    }

    protected final void appendAsDataNode(SyncNode node) {
        node.state = null;
        node.thread = null;
        waitChain.offer(node);
    }

    protected final boolean appendAsWaitNode(SyncNode node) {
        node.setCurrentThread();
        waitChain.offer(node);
        return node == waitChain.peek();
    }

    //****************************************************************************************************************//
    //                                          2: wakeup for result wait pool(2)                                     //
    //****************************************************************************************************************//
    public final void wakeupFirst(Object wakeupType) {
        SyncNode first = waitChain.peek();
        if (first != null && (wakeupType == null || wakeupType == first.type || wakeupType.equals(first.type)))
            if (first.state == null && casState(first, null, RUNNING)) LockSupport.unpark(first.thread);
    }

    protected final void removeAndWakeupFirst(SyncNode node, boolean wakeup, Object wakeupType) {
        waitChain.remove(node);
        if (wakeup && (node = waitChain.peek()) != null) {
            if (wakeupType == null || wakeupType == node.type || wakeupType.equals(node.type))
                if (node.state == null && casState(node, null, RUNNING)) LockSupport.unpark(node.thread);
        }
    }

    //****************************************************************************************************************//
    //                                          3: wakeup(2)                                                          //
    //****************************************************************************************************************//
    public final SyncNode wakeupOne(boolean fromHead, Object nodeType, Object toState) {
        //Iterator<SyncNode> iterator = fromHead ? waitChain.iterator() : waitChain.descendingIterator();
        Iterator<SyncNode> iterator = waitChain.iterator();

        //2: retrieve type matched node and unpark its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if (casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                    return qNode;
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if ((nodeType == qNode.type || nodeType.equals(qNode.type)) && casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                    return qNode;
                }
            }
        }
        //3: not found matched node
        return null;
    }

    public final void wakeupAll(boolean fromHead, Object nodeType, Object toState) {
        //Iterator<SyncNode> iterator = fromHead ? waitChain.iterator() : waitChain.descendingIterator();
        Iterator<SyncNode> iterator = waitChain.iterator();

        //2: retrieve type matched node and unpark its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if (casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if ((nodeType == qNode.type || nodeType.equals(qNode.type)) && casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                }
            }
        }
    }

    //****************************************************************************************************************//
    //                                         4: Monitor Methods(6)                                                  //
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
