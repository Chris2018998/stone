/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import java.util.Iterator;
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
public abstract class WaitConditionPool extends SynThreadWaitPool {

    public abstract boolean testCondition();

    public Object get(Object arg, ThreadParker parker) throws InterruptedException, TimeoutException {
        //1: test condition,if true then return true directly
        if (testCondition()) return true;

        //2:create wait node and offer to wait queue
        ThreadNode node = new ThreadNode();
        node.setValue(arg);
        waitQueue.offer(node);
        boolean isTimeout = false;
        boolean isInterrupted = false;
        boolean allowThrowInterruptedException = parker.allowThrowInterruptedException();

        //3:spin control
        while (true) {
            Object state = node.getState();
            if (state == NOTIFIED) {//notified
                if (isInterrupted) {

                } else if (isTimeout) {

                } else if (testCondition()) {
                    waitQueue.remove(node);
                    return NOTIFIED;
                }
            }

            //park current thread
            parker.calNextParkTime();
            if (parker.isTimeout()) {
                isTimeout = true;
            } else {
                isInterrupted = parker.park();
            }
        }
    }

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
}
