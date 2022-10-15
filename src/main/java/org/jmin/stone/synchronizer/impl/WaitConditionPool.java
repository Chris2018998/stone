/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import java.util.concurrent.TimeoutException;

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
                if (casNodeState(node, state, TIMEOUT)) {
                    waitQueue.remove(node);
                    throw new TimeoutException();
                }
            } else if (parker.park()) {
                isInterrupted = true;
                if (allowThrowInterruptedException && casNodeState(node, state, INTERRUPTED)) {
                    waitQueue.remove(node);
                    throw new InterruptedException();
                }
            }
        }
    }
}
