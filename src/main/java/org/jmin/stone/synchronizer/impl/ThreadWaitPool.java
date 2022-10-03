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
        } else if (node.getState() != NOTIFIED) {//timeout
            casNodeState(node, WAITING, TIMEOUT);
            this.removeNode(node);
            throw new TimeoutException();
        }
    }

    //****************************************************************************************************************//
    //                                          2: wakeup methods                                                     //
    //****************************************************************************************************************//
    //wakeup all nodes and clean chain
    protected void wakeupAll() {
        //@todo
    }

    //wakeup nodes with type value and remove them
    protected int wakeupByType(long typeCode) {
        int count = 0;
        //@todo
        return count;
    }

    //wakeup all nodes and clean chain,count node witch type value
    protected int wakeupAllAndCountType(long typeCode) {
        int count = 0;
        //@todo
        return count;
    }
}
