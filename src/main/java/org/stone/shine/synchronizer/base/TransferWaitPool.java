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

    //transfer a object to waiter
    public final boolean tryTransfer(Object object) {
        return super.wakeupOneToState(object) == 1;
    }

    /**
     * get an transferred object from pool
     *
     * @param throwsIE true,throws InterruptedException when interrupted
     * @return boolean value,true means wakeup with expect signal state,false,wait timeout
     * @throws InterruptedException throw it when throwsIE parameter is true and thread interrupted
     */
    public final Object get(ThreadParkSupport support, boolean throwsIE) throws InterruptedException {
        //1:create wait node and offer to wait queue
        ThreadNode node = super.appendNewNode();

        //2:spin control
        try {
            do {
                //2.1:read state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) return state;

                //here:null state
                if (support.getParkTime() <= 0) {//timeout
                    //two types(1:null state 2:invalid state,not expected state)
                    if (ThreadNodeUpdater.casNodeState(node, null, ThreadNodeState.TIMEOUT)) return null;
                } else {
                    //2.2: park current thread
                    parkNodeThread(node, support, throwsIE);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
