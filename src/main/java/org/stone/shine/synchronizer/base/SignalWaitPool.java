/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.synchronizer.base;

import org.stone.shine.synchronizer.ThreadNode;
import org.stone.shine.synchronizer.ThreadNodeState;
import org.stone.shine.synchronizer.ThreadParkSupport;
import org.stone.shine.synchronizer.ThreadWaitPool;

import java.util.concurrent.TimeoutException;

/**
 * wait util SIGNAL wakeup
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SignalWaitPool extends ThreadWaitPool {

    //wait util wakeup or timeout
    public final void await(ThreadParkSupport support, boolean throwsIE) throws InterruptedException, TimeoutException {
        await(support, throwsIE, null);
    }

    //wait util wakeup or timeout
    public final void await(ThreadParkSupport support, boolean throwsIE, Object nodeValue) throws InterruptedException, TimeoutException {
        //1:create wait node and offer to wait queue
        ThreadNode node = super.appendNewNode(nodeValue);

        //2:spin control
        try {
            do {
                //2.1:read state
                if (node.getState() == ThreadNodeState.SIGNAL) return;
                //2.2: park current thread
                parkNodeThread(node, support, throwsIE);
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
