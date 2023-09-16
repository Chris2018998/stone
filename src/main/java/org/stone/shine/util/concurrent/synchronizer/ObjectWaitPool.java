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

import org.stone.shine.util.concurrent.synchronizer.chain.SyncNode;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.unpark;
import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeStates.REMOVED;
import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeStates.RUNNING;
import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.objectEquals;

/**
 * Wait-Wakeup is a import mechanism for threads, wait for what,maybe a signal,a command,a permit and so on.
 * Every thing is object in Java world, so we name the pool: Object Wait Pool.
 * <p>
 * A Joke from a thread: I am waiting for a Object from the world,but blocking time too long.
 *
 * @author Chris Liao
 * @version 1.0
 */

abstract class ObjectWaitPool {
    protected final Queue<SyncNode> waitQueue;//node wait queue

    //****************************************************************************************************************//
    //                                          1:constructors(2)                                                     //
    //****************************************************************************************************************//
    ObjectWaitPool() {
        this.waitQueue = new ConcurrentLinkedQueue<>();
    }

    ObjectWaitPool(Queue<SyncNode> waitQueue) {
        this.waitQueue = waitQueue;
    }

    //****************************************************************************************************************//
    //                                          2: append(3)                                                          //
    //****************************************************************************************************************//
    final SyncNode peekFirst() {
        return waitQueue.peek();
    }

    final void appendAsDataNode(SyncNode node) {
        node.setState(null);
        node.setThread(null);
        waitQueue.offer(node);
    }

    final boolean appendAsWaitNode(SyncNode node) {
        node.setThread(currentThread());
        waitQueue.offer(node);
        return node == waitQueue.peek();
    }

    //****************************************************************************************************************//
    //                                          3: wakeup for result wait pool(3)                                     //
    //****************************************************************************************************************//
    final void removeAndWakeupFirst(SyncNode node) {
        waitQueue.remove(node);
        wakeupFirst();
    }

    public final void wakeupFirst() {
        SyncNode first = waitQueue.peek();
        if (first != null && casState(first, null, RUNNING))
            unpark(first.getThread());
    }

    public final void wakeupFirst(Object wakeupType) {
        SyncNode first = waitQueue.peek();
        if (first != null && (wakeupType == null || wakeupType == first.getType() || wakeupType.equals(first.getType())))
            if (casState(first, null, RUNNING)) unpark(first.getThread());
    }

    //****************************************************************************************************************//
    //                                          4: wakeup(2)                                                          //
    //****************************************************************************************************************//
    public final SyncNode wakeupOne(boolean fromHead, Object nodeType, Object toState) {
        Iterator<SyncNode> iterator = fromHead ? waitQueue.iterator() : descendingIterator();

        //2: retrieve type matched node and un-park its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if (casState(qNode, null, toState)) {
                    unpark(qNode.getThread());
                    return qNode;
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if ((nodeType == qNode.getType() || nodeType.equals(qNode.getType())) && casState(qNode, null, toState)) {
                    unpark(qNode.getThread());
                    return qNode;
                }
            }
        }
        //3: not found matched node
        return null;
    }

    public final void wakeupAll(boolean fromHead, Object nodeType, Object toState) {
        Iterator<SyncNode> iterator = fromHead ? waitQueue.iterator() : descendingIterator();

        //2: retrieve type matched node and un-park its thread
        if (nodeType == null) {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if (casState(qNode, null, toState)) {
                    unpark(qNode.getThread());
                }
            }
        } else {
            while (iterator.hasNext()) {
                SyncNode qNode = iterator.next();
                if (qNode.getState() == REMOVED) {
                    iterator.remove();
                } else if ((nodeType == qNode.getType() || nodeType.equals(qNode.getType())) && casState(qNode, null, toState)) {
                    unpark(qNode.getThread());
                }
            }
        }
    }

    //****************************************************************************************************************//
    //                                         5: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
    Iterator<SyncNode> ascendingIterator() {
        return waitQueue.iterator();
    }

    private Iterator<SyncNode> descendingIterator() {
        if (waitQueue instanceof Deque)
            return ((Deque<SyncNode>) waitQueue).descendingIterator();
        else
            throw new UnsupportedOperationException("Current queue is not a deque implementation");
    }

    final boolean existsTypeNode(Object nodeType) {
        for (SyncNode node : waitQueue) {
            if (objectEquals(nodeType, node.getType())) return true;
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        for (SyncNode node : waitQueue) {
            Object state = node.getState();
            if ((state == null || state == RUNNING) && node.getThread() != null) return true;
        }
        return false;
    }

    public final boolean hasQueuedThread(Thread thread) {
        for (SyncNode node : waitQueue) {
            Object state = node.getState();
            if ((state == null || state == RUNNING) && node.getThread() == thread) return true;
        }
        return false;
    }

    public final int getQueueLength() {
        try {
            int count = 0;
            for (SyncNode node : waitQueue) {
                Object state = node.getState();
                if (state == null || state == RUNNING) count++;
            }
            return count;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    public final Collection<Thread> getQueuedThreads() {
        LinkedList<Thread> threadList = new LinkedList<>();
        for (SyncNode node : waitQueue) {
            Object state = node.getState();
            if ((state == null || state == RUNNING) && node.getThread() != null) threadList.add(node.getThread());
        }
        return threadList;
    }

    protected int getQueueLength(Object nodeType) {
        if (nodeType == null) return getQueueLength();

        int count = 0;
        for (SyncNode node : waitQueue) {
            Object state = node.getState();
            if ((state == null || state == RUNNING) && objectEquals(nodeType, node.getType())) count++;
        }
        return count;
    }

    protected Collection<Thread> getQueuedThreads(Object nodeType) {
        if (nodeType == null) return getQueuedThreads();

        LinkedList<Thread> threadList = new LinkedList<>();
        for (SyncNode node : waitQueue) {
            Object state = node.getState();
            if ((state == null || state == RUNNING) && objectEquals(nodeType, node.getType()) && node.getThread() != null)
                threadList.add(node.getThread());
        }
        return threadList;
    }
}
