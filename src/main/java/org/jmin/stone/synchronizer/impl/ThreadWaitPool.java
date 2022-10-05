/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import static org.jmin.stone.synchronizer.impl.ThreadNodeState.*;

/**
 * Wait Pool,threads will leave from pool under three situation
 * 1: wakeup by other thread
 * 2: wait timeout
 * 3: wait interrupted
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class ThreadWaitPool extends ThreadNodeChain {
    //****************************************************************************************************************//
    //                                          1: wait methods                                                       //
    //****************************************************************************************************************//
    protected void doWait(long timeoutNs) throws InterruptedException, TimeoutException {
        doWait(timeoutNs, 0);
    }

    protected void doWait(long timeoutNs, long waitType) throws InterruptedException, TimeoutException {
        //1:create node and add to chain
        ThreadNode node = new ThreadNode(WAITING);
        node.setType(waitType);
        this.addNode(node);

        //2:park current thread(wait)
        ThreadParker.create(timeoutNs, false).park();

        //3:after exiting park
        if (node.getThread().isInterrupted()) {//interrupted
            casNodeState(node, WAITING, INTERRUPTED);
            this.removeNode(node);
            throw new InterruptedException();
        } else if (node.getState() == WAITING) {//timeout(state not changed)
            casNodeState(node, WAITING, TIMEOUT);
            this.removeNode(node);
            throw new TimeoutException();
        }
    }

    //****************************************************************************************************************//
    //                                          2: wakeup methods                                                     //
    //****************************************************************************************************************//
    //poll all thread nodes and wakeup them if in waiting
    protected void wakeupAll() {
        ThreadNode node;
        while ((node = super.pollFirst()) != null) {
            if (node.getState() == WAITING && casNodeState(node, WAITING, NOTIFIED)) {
                if (!node.getThread().isInterrupted()) LockSupport.unpark(node.getThread());
            }
        }
    }

    //poll all thread nodes and wakeup them if in waiting and count by specified node type
    protected int wakeupAllAndCountType(long typeCode) {
        int count = 0;
        ThreadNode node;
        while ((node = super.pollFirst()) != null) {
            if (node.getState() == WAITING && casNodeState(node, WAITING, NOTIFIED)) {
                if (!node.getThread().isInterrupted()) {
                    LockSupport.unpark(node.getThread());
                    if (node.getType() == typeCode) count++;
                }
            }
        }
        return count;
    }

    //find out all specified type node,if in waiting then wakeup them
    protected int wakeupByType(long typeCode) {
        int count = 0;
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (node.getType() == typeCode && state == ThreadNodeState.WAITING && casNodeState(node, WAITING, NOTIFIED)) {
                if (!node.getThread().isInterrupted()) {
                    LockSupport.unpark(node.getThread());
                    if (node.getType() == typeCode) count++;
                    super.removeNode(node);
                }
            }
        }
        return count;
    }
}
