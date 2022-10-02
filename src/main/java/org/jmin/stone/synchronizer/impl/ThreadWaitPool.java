/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    //                                          1:abstract methods                                                    //
    //****************************************************************************************************************//
    //if true,<method>{@link #wakeupWaiting}</method>should be called by manually or auto
    public abstract boolean testCondition();

    //****************************************************************************************************************//
    //                                          2: wakeup methods                                                     //
    //****************************************************************************************************************//
    //wakeup all nodes and clean chain
    protected void wakeupAll() {

    }

    //wakeup nodes with type value and remove them
    protected int wakeupByType(long typeCode) {
        int count = 0;

        return count;
    }

    //wakeup all nodes and clean chain,count node witch type value
    protected int wakeupAllAndCountType(long typeCode) {
        int count = 0;

        return count;
    }

    //****************************************************************************************************************//
    //                                          3: wait methods                                                       //
    //****************************************************************************************************************//
    protected void doWait() throws InterruptedException {
        this.doWait(0);
    }

    protected void doWait(long waitType) throws InterruptedException {
        try {
            doWait(0, waitType);
        } catch (TimeoutException e) {
            //do nothing
        }
    }

    //time wait
    protected void doWait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        this.doWait(timeout, unit, 0);
    }

    //time wait with wait type()
    protected void doWait(long timeout, TimeUnit unit, long waitType) throws InterruptedException, TimeoutException {
        doWait(unit.toNanos(timeout), waitType);
    }

    //wait implement method
    private void doWait(long timeoutNs, long waitType) throws InterruptedException, TimeoutException {
        if (!testCondition()) {
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
            } else if (node.getState() != NOTIFIED) {//timeout
                casNodeState(node, WAITING, TIMEOUT);
                this.removeNode(node);
                throw new TimeoutException();
            }
        }
    }
}
