/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.synchronizer.base;

import org.stone.shine.synchronizer.*;

/**
 * wait util wakeup
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SignalWaitPool extends ThreadWaitPool {

    /**
     * add to inner queue and wait util wakeup by other thread with default signal state
     *
     * @param throwsIE true,throws InterruptedException when interrupted
     * @return boolean value,true means wakeup with expect signal state,false,wait timeout
     * @throws InterruptedException throw it when throwsIE parameter is true and thread interrupted
     */
    public final boolean doWait(ThreadParkSupport support, boolean throwsIE) throws InterruptedException {
        return doWait(support, throwsIE, null);
    }

    /**
     * add to inner queue and wait util wakeup by other thread with expect signal state
     *
     * @param support   thread park support(@see #ThreadParkSupport)
     * @param nodeValue a property value in ThreadNode
     * @param throwsIE  true,throws InterruptedException when interrupted
     * @return boolean value,true means wakeup with expect signal state,false,wait timeout
     * @throws InterruptedException throw it when throwsIE parameter is true and thread interrupted
     */
    public final boolean doWait(ThreadParkSupport support, boolean throwsIE, Object nodeValue) throws InterruptedException {
        //1:create wait node and offer to wait queue
        ThreadNode node = super.appendNewNode(nodeValue);

        //2:spin control
        try {
            do {
                //2.1:read state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) return true;

                //here:null state
                if (support.getParkTime() <= 0) {//timeout
                    //two types(1:null state 2:invalid state,not expected state)
                    if (ThreadNodeUpdater.casNodeState(node, null, ThreadNodeState.TIMEOUT)) return false;
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
