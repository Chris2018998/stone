/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.base;

import org.stone.shine.synchronizer.*;

/**
 * transfer wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TransferWaitPool extends ThreadWaitPool {
    //true,use fair mode to execute call
    private boolean fair;

    //****************************************************************************************************************//
    //                                          1: constructors(2)                                                    //
    //****************************************************************************************************************//
    public TransferWaitPool() {
    }

    public TransferWaitPool(boolean fair) {
        this.fair = fair;
        //true:transfer from head,which similar to{@link java.util.concurrent.SynchronousQueue#TransferQueue}
        //false:transfer from tail,which similar to{@link java.util.concurrent.SynchronousQueue#TransferStack}
    }

    public boolean isFair() {
        return this.fair;
    }

    //****************************************************************************************************************//
    //                                          2: get/transfer methods                                               //
    //****************************************************************************************************************//
    //transfer a object to waiter
    public final boolean tryTransfer(Object object) {
        return super.wakeupOne(object) == 1;
    }

    /**
     * try to get an transferred object from pool
     *
     * @param parker   thread parker
     * @param throwsIE true,throws InterruptedException when interrupted
     * @return boolean value,true means wakeup with expect signal state,false,wait timeout
     * @throws InterruptedException throw it when throwsIE parameter is true and thread interrupted
     */
    public final Object get(ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        //1:create wait node and offer to wait queue
        ThreadNode node = super.appendNewNode();

        //2:spin control
        try {
            do {
                //2.1: read node state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) return state;

                //2.2: timeout test
                if (parker.isTimeout()) {
                    //2.2.1: try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return null
                    if (ThreadNodeUpdater.casNodeState(node, null, ThreadNodeState.TIMEOUT)) return null;
                } else {
                    //2.3: park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, parker, throwsIE, true);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
