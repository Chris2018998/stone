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
public class TransferWaitPool<E> extends ThreadWaitPool {
    //Request
    private static final Object Node_Type_Get = new Object();
    //Data
    private static final Object Node_Type_Transfer = new Object();

    //true,use fair mode to execute call
    private boolean fair;

    //****************************************************************************************************************//
    //                                          1: constructors(2)                                                    //
    //****************************************************************************************************************//
    public TransferWaitPool() {
        this(true);
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
    //                                          2: transfer methods                                                   //
    //****************************************************************************************************************//
    public final boolean tryTransfer(E e) {
        if (e == null) throw new NullPointerException();
        return this.wakeupOne(fair, e, Node_Type_Get) == 1;
    }

    //transfer a object to waiter
    public final boolean transfer(E e, ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        if (e == null) throw new NullPointerException();

        //step1: try to transfer
        if (this.tryTransfer(e)) return true;

        //step2:create wait node(then to wait)
        ThreadNode node = super.createNode(Node_Type_Transfer);

        //step3:create wait node(then to wait)
        return doWait(node, parker, throwsIE) != null;
    }

    //****************************************************************************************************************//
    //                                          3: get methods                                                        //
    //****************************************************************************************************************//
    public final E tryGet() {
        ThreadNode node = this.getWokenUpNode(fair, ThreadNodeState.SIGNAL, Node_Type_Transfer);
        return node != null ? (E) node.getValue() : null;
    }

    public final E get(ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        //step1: try to get
        E e = tryGet();
        if (e != null) return e;

        //step2:create wait node(then to wait)
        ThreadNode node = super.createNode(Node_Type_Get);

        //step3:create wait node(then to wait)
        return (E) doWait(node, parker, throwsIE);
    }

    //****************************************************************************************************************//
    //                                          4: core methods                                                       //
    //****************************************************************************************************************//
    private final Object doWait(ThreadNode node, ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        super.appendNode(node);

        try {
            do {
                //1.1: read node state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) {//wokenUp
                    if (node.getType() == Node_Type_Transfer) {
                        return node;//that means transferred object has been got by other
                    } else {//state==Node_Type_Get
                        return state;
                    }
                }

                //1.2: timeout test
                if (parker.isTimeout()) {
                    //1.2.1: try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return null
                    if (ThreadNodeUpdater.casNodeState(node, null, ThreadNodeState.TIMEOUT)) return null;
                } else {
                    //1.3: park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, parker, throwsIE, true);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
