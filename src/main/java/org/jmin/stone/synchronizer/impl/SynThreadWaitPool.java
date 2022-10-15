/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.stone.synchronizer.ThreadWaitPool;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import static org.jmin.stone.synchronizer.impl.ThreadNodeState.*;
import static org.jmin.stone.synchronizer.impl.ThreadNodeUpdater.casNodeState;

/**
 * get notification,message,command or other
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class SynThreadWaitPool implements ThreadWaitPool {

    //temp util ThreadNodeChain is stable
    protected Queue<ThreadNode> waitQueue = new ConcurrentLinkedQueue<>();

    //****************************************************************************************************************//
    //                                          1: get Methods                                                        //
    //****************************************************************************************************************//
    //ignore interruption
    public Object getUninterruptibly(Object arg) {
        try {
            ThreadParker parker = ThreadParker.create(0, false);
            parker.setAutoClearInterruptedFlag(true);
            return get(arg, parker);
        } catch (TimeoutException e) {
            //in fact,TimeoutException never be thrown out here
            throw new Error(e);
        } catch (InterruptedException e) {
            //in fact,InterruptedException never be thrown out here
            throw new Error(e);
        }
    }

    //throws InterruptedException if the current thread is interrupted while getting
    public Object get(Object arg) throws InterruptedException {
        try {
            return get(arg, ThreadParker.create(0, false));
        } catch (TimeoutException e) {
            //in fact,TimeoutException never be thrown out here
            throw new Error(e);
        }
    }

    //if got failed,then causes the current thread to wait until interrupted or timeout
    public Object get(Object arg, Date utilDate) throws InterruptedException, TimeoutException {
        return get(arg, ThreadParker.create(utilDate.getTime(), true));
    }

    //if got failed,then causes the current thread to wait until interrupted or timeout
    public Object get(Object arg, long timeOut, TimeUnit unit) throws InterruptedException, TimeoutException {
        return get(arg, ThreadParker.create(unit.toNanos(timeOut), false));
    }

    //need implement in sub class
    public abstract Object get(Object type, ThreadParker parker) throws InterruptedException, TimeoutException;

    //****************************************************************************************************************//
    //                                          2: Wakeup methods                                                     //
    //****************************************************************************************************************//
    public int wakeupAll() {
        int count = 0;
        ThreadNode node;
        while ((node = waitQueue.poll()) != null) {
            Object state = node.getState();
            if ((state == RUNNING || state == WAITING) && casNodeState(node, state, NOTIFIED)) {
                count++;
                if (state == WAITING) LockSupport.unpark(node.getThread());
            }
        }
        return count;
    }

    public int wakeup(Object arg) {
        int count = 0;
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            Object state = node.getState();
            if (!arg.equals(node.getValue())) continue;
            if ((state == RUNNING || state == WAITING) && casNodeState(node, state, NOTIFIED)) {
                count++;
                if (state == WAITING) LockSupport.unpark(node.getThread());
            }
        }
        return count;
    }

    protected int wakeupOne(Object arg, ThreadNode exclusiveNode) {
        int count = 0;
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (node == exclusiveNode) continue;
            Object state = node.getState();
            if (!arg.equals(node.getValue())) continue;
            if ((state == RUNNING || state == WAITING) && casNodeState(node, state, NOTIFIED)) {
                count++;
                if (state == WAITING) LockSupport.unpark(node.getThread());
            }
        }
        return count;
    }

    //****************************************************************************************************************//
    //                                         3: Monitor Methods(State in(RUNNING,WAITING)                           //
    //****************************************************************************************************************//
    public final int getQueueLength() {
        int count = 0;
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            Object state = node.getState();
            if (state == RUNNING || state == WAITING) count++;
        }
        return count;
    }

    public Collection<Thread> getQueuedThreads() {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            Object state = node.getState();
            if (state == RUNNING || state == WAITING) threadList.add(node.getThread());
        }
        return threadList;
    }

    public final int getQueueLength(Object arg) {
        int count = 0;
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            Object state = node.getState();
            if (arg.equals(node.getValue()) && (state == RUNNING || state == WAITING)) count++;
        }
        return count;
    }

    public Collection<Thread> getQueuedThreads(Object arg) {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            Object state = node.getState();
            if (arg.equals(node.getValue()) && (state == RUNNING || state == WAITING)) threadList.add(node.getThread());
        }
        return threadList;
    }
}
