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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.objectEquals;

/**
 * Base Wait-Wakeup Pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class ThreadWaitingPool<E> {
    //private final SyncNodeChain waitChain = new SyncNodeChain();
    private final ConcurrentLinkedDeque<SyncNode> waitChain = new ConcurrentLinkedDeque<>();//temporary

    //****************************************************************************************************************//
    //                                          1: queue Methods(4)                                                   //
    //****************************************************************************************************************//
    protected final void removeNode(SyncNode<E> node) {
        waitChain.remove(node);
    }

    protected final SyncNode<E> appendAsWaitNode(SyncNode node) {
        node.setOwnerThread();
        waitChain.offer(node);
        return node;
    }

    protected final SyncNode<E> appendAsDataNode(Object state, Object type, E value) {
        SyncNode node = new SyncNode<E>(state, type, value);
        waitChain.offer(node);
        return node;
    }

    //****************************************************************************************************************//
    //                                          2: wakeup(2)                                                          //
    //****************************************************************************************************************//
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
                if (nodeType == qNode.type && casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                    return qNode;
                }
            }
        }
        //3: not found matched node
        return null;
    }

    public final int wakeupAll(boolean fromHead, Object nodeType, Object toState) {
        int wakeupCount = 0;
        Iterator<SyncNode> iterator = fromHead ? waitChain.iterator() : waitChain.descendingIterator();

        //2: retrieve type matched node and unpark its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                    wakeupCount++;
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (nodeType == qNode.type && casState(qNode, null, toState)) {
                    LockSupport.unpark(qNode.thread);
                    wakeupCount++;
                }
            }
        }
        return wakeupCount;
    }

    //****************************************************************************************************************//
    //                                          3: leave from pool(1)                                                 //
    //****************************************************************************************************************//
    protected final SyncNode leaveFromWaitQueue(SyncNode current, boolean wakeup, boolean fromHead, Object nodeType, Object toState) {
        Iterator<SyncNode> iterator = fromHead ? waitChain.iterator() : waitChain.descendingIterator();

        //1: remove current node(wakeup occurred by this)
        while (iterator.hasNext()) {
            SyncNode qNode = iterator.next();
            if (current == qNode) {
                iterator.remove();
                break;
            }
        }

        if (wakeup) {
            iterator = fromHead ? waitChain.iterator() : waitChain.descendingIterator();
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
                    if (nodeType == qNode.type && casState(qNode, null, toState)) {
                        LockSupport.unpark(qNode.thread);
                        return qNode;
                    }
                }
            }
        }

        //3: not found matched node
        return null;
    }


    //****************************************************************************************************************//
    //                                         4: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
    protected final Iterator<SyncNode> ascendingIterator() {
        return waitChain.iterator();
    }

    public final boolean hasQueuedPredecessors() {
        SyncNode node = waitChain.peek();
        return node != null && node.thread != Thread.currentThread();
    }

    protected final boolean existsTypeNode(Object nodeType) {
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (nodeType == null || objectEquals(nodeType, node.type)) return true;
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
