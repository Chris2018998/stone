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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.util.concurrent.locks.LockSupport.unpark;
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

public abstract class SyncNodeWaitPool {
    //node wait queue
    protected final Queue<SyncNode> waitQueue;

    //****************************************************************************************************************//
    //                                          1:constructors(2)                                                     //
    //****************************************************************************************************************//
    public SyncNodeWaitPool() {
        this.waitQueue = new ConcurrentLinkedQueue<>();
    }

    public SyncNodeWaitPool(Queue<SyncNode> waitQueue) {
        this.waitQueue = waitQueue;
    }

    //****************************************************************************************************************//
    //                                          2: append(2)                                                          //
    //****************************************************************************************************************//
    protected final void appendAsDataNode(SyncNode node) {
        node.state = null;
        node.thread = null;
        waitQueue.offer(node);
    }

    protected final boolean appendAsWaitNode(SyncNode node) {
        node.thread = Thread.currentThread();
        waitQueue.offer(node);
        return node == waitQueue.peek();
    }

    //****************************************************************************************************************//
    //                                          3: wakeup for result wait pool(3)                                     //
    //****************************************************************************************************************//
    public final void wakeupFirst() {
        SyncNode first = waitQueue.peek();
        if (first != null && casState(first, null, RUNNING))
            unpark(first.thread);
    }

    public final void wakeupFirst(Object wakeupType) {
        SyncNode first = waitQueue.peek();
        if (first != null && (wakeupType == null || wakeupType == first.type || wakeupType.equals(first.type)))
            if (casState(first, null, RUNNING)) unpark(first.thread);
    }

    protected final void wakeupFirstOnFailure(SyncNode node, boolean atFirst) {
        if (atFirst) {
            waitQueue.poll();
            node = waitQueue.peek();
            if (node != null && casState(node, null, RUNNING)) unpark(node.thread);
        } else {
            waitQueue.remove(node);
        }
    }

    //****************************************************************************************************************//
    //                                          4: wakeup(2)                                                          //
    //****************************************************************************************************************//
    public final SyncNode wakeupOne(boolean fromHead, Object nodeType, Object toState) {
        Iterator<SyncNode> iterator = fromHead ? waitQueue.iterator() : descendingIterator();

        //2: retrieve type matched node and unpark its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if (casState(qNode, null, toState)) {
                    unpark(qNode.thread);
                    return qNode;
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if ((nodeType == qNode.type || nodeType.equals(qNode.type)) && casState(qNode, null, toState)) {
                    unpark(qNode.thread);
                    return qNode;
                }
            }
        }
        //3: not found matched node
        return null;
    }

    public final void wakeupAll(boolean fromHead, Object nodeType, Object toState) {
        Iterator<SyncNode> iterator = fromHead ? waitQueue.iterator() : descendingIterator();

        //2: retrieve type matched node and unpark its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if (casState(qNode, null, toState)) {
                    unpark(qNode.thread);
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if ((nodeType == qNode.type || nodeType.equals(qNode.type)) && casState(qNode, null, toState)) {
                    unpark(qNode.thread);
                }
            }
        }
    }

    //****************************************************************************************************************//
    //                                         5: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
    protected final Iterator<SyncNode> ascendingIterator() {
        return waitQueue.iterator();
    }

    protected final Iterator<SyncNode> descendingIterator() {
        if (waitQueue instanceof Deque)
            return ((Deque<SyncNode>) waitQueue).descendingIterator();
        else
            throw new UnsupportedOperationException("Current Queue is not a implementation of Deque");
    }

    protected final boolean existsTypeNode(Object nodeType) {
        Iterator<SyncNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (objectEquals(nodeType, node.type)) return true;
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        Iterator<SyncNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null && node.thread != null) return true;
        }
        return false;
    }

    public final boolean hasQueuedThread(Thread thread) {
        Iterator<SyncNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.thread == thread) return true;
        }
        return false;
    }

    public final int getQueueLength() {
        int count = 0;
        Iterator<SyncNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null) count++;
        }
        return count;
    }

    public final Collection<Thread> getQueuedThreads() {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<SyncNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null && node.thread != null) threadList.add(node.thread);
        }
        return threadList;
    }

    protected int getQueueLength(Object nodeType) {
        if (nodeType == null) return getQueueLength();

        int count = 0;
        Iterator<SyncNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (objectEquals(nodeType, node.type) && node.state == null) count++;
        }
        return count;
    }

    protected Collection<Thread> getQueuedThreads(Object nodeType) {
        if (nodeType == null) return getQueuedThreads();

        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<SyncNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (objectEquals(nodeType, node.type) && node.state == null && node.thread != null)
                threadList.add(node.thread);
        }
        return threadList;
    }
}
