/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.base;

import org.stone.shine.util.concurrent.synchronizer.SyncNode;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.ThreadParkSupport;
import org.stone.shine.util.concurrent.synchronizer.ThreadWaitingPool;

import java.util.Iterator;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.INTERRUPTED;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.TIMEOUT;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.maxTimedSpins;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * transfer wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TransferWaitPool<E> extends ThreadWaitingPool<E> {
    //Request
    private static final Object Node_Type_Get = new Object();
    //Data
    private static final Object Node_Type_Data = new Object();
    //true,use fair mode to execute call
    private final boolean fair;

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

    public final boolean isFair() {
        return this.fair;
    }

    //****************************************************************************************************************//
    //                                          1: offer methods(3)                                                   //
    //****************************************************************************************************************//
    public final boolean offer(E e) {
        if (tryTransfer(e)) return true;

        this.appendAsDataNode(null, Node_Type_Data, e);
        return false;
    }

    public final boolean offer(E e, SyncVisitConfig<E> config) {
        try {
            if (transfer(e, config)) return true;
        } catch (InterruptedException ex) {
            //do nothing
        }

        this.appendAsDataNode(null, Node_Type_Data, e);
        return false;
    }

    //****************************************************************************************************************//
    //                                          2:transfer methods                                                    //
    //****************************************************************************************************************//
    public final boolean tryTransfer(E e) {
        if (e == null) throw new NullPointerException();
        return this.wakeupOne(fair, Node_Type_Get, e) != null;
    }

    //transfer a object to waiter
    public final boolean transfer(E e, SyncVisitConfig<E> config) throws InterruptedException {
        if (e == null) throw new NullPointerException();

        //step1: try to transfer
        if (this.tryTransfer(e)) return true;

        //step2:create wait node(then to wait)
        config.setNodeInitInfo(Node_Type_Data, e);

        //step3:create wait node(then to wait)
        return doWait(config) != null;
    }

    //****************************************************************************************************************//
    //                                          3: get methods                                                        //
    //****************************************************************************************************************//
    public final E tryGet() {
        SyncNode<E> node = new SyncNode<E>(null, Node_Type_Get, null);
        SyncNode<E> wakeNode = this.wakeupOne(fair, Node_Type_Data, node);
        return wakeNode != null ? wakeNode.getValue() : null;
    }

    public final E get(SyncVisitConfig<E> config) throws InterruptedException {
        //step1: try to get
        E e = tryGet();
        if (e != null) return e;

        //step2:create wait node(then to wait)
        config.setNodeType(Node_Type_Get);

        //step3:create wait node(then to wait)
        return (E) doWait(config);
    }

    //****************************************************************************************************************//
    //                                          4: core methods                                                       //
    //****************************************************************************************************************//
    private Object doWait(SyncVisitConfig<E> config) throws InterruptedException {
        if (config == null) throw new IllegalArgumentException("wait config can't be null");
        if (Thread.interrupted()) throw new InterruptedException();

        //1:create wait node and offer to wait queue
        SyncNode node = config.getSyncNode();
        int spins = appendAsWaitNode(node) ? maxTimedSpins : 0;//spin count

        //2:get control parameters from config
        boolean allowInterrupted = config.supportInterrupted();
        ThreadParkSupport parkSupport = config.getParkSupport();

        //3:spin control
        try {
            do {
                //3.1: read node state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) {//wokenUp
                    if (state == TIMEOUT) return null;
                    if (state == INTERRUPTED) throw new InterruptedException();
                    if (node.getType() == Node_Type_Data) {
                        return node;//that means transferred object has been got by other
                    } else {//state==Node_Type_Get
                        return state;
                    }
                }

                //4.3: fail check
                if (parkSupport.isTimeout()) {
                    casState(node, null, TIMEOUT);
                } else if (parkSupport.isInterrupted() && allowInterrupted) {
                    casState(node, null, INTERRUPTED);
                } else if (spins > 0) {
                    --spins;
                } else if (parkSupport.computeParkNanos() > spinForTimeoutThreshold) {
                    parkSupport.park();
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }

    //****************************************************************************************************************//
    //                                          5: monitor methods(4)                                                 //
    //****************************************************************************************************************//
    public boolean isEmpty() {
        return !super.existsTypeNode(Node_Type_Data);
    }

    public boolean hasWaitingConsumer() {
        return super.existsTypeNode(Node_Type_Get);
    }

    public int getWaitingConsumerCount() {
        return super.getQueueLength(Node_Type_Get);
    }

    public int size() {
        return super.getQueueLength(Node_Type_Data);
    }

    //****************************************************************************************************************//
    //                                          6: iterator methods(4)                                                //
    //****************************************************************************************************************//
    public E peek() {
        Iterator<SyncNode> nodeIterator = super.ascendingIterator();
        while (nodeIterator.hasNext()) {
            SyncNode node = nodeIterator.next();
            if (Node_Type_Data == node.getType()) return (E) node.getValue();
        }
        return null;
    }

    public boolean remove(Object o) {
        if (o == null) return false;
        Iterator<SyncNode> nodeIterator = super.ascendingIterator();
        while (nodeIterator.hasNext()) {
            SyncNode node = nodeIterator.next();
            if (node.getType() == Node_Type_Data && o.equals(node.getValue())) {
                nodeIterator.remove();
                return true;
            }
        }
        return false;
    }

    public boolean contains(Object o) {
        if (o == null) return false;
        Iterator<SyncNode> nodeIterator = super.ascendingIterator();
        while (nodeIterator.hasNext()) {
            SyncNode node = nodeIterator.next();
            if (node.getType() == Node_Type_Data && o.equals(node.getValue())) return true;
        }
        return false;
    }

    public Iterator<E> iterator() {
        return new DataIterator<E>(super.ascendingIterator());
    }

    private static class DataIterator<E> implements Iterator {
        private final Iterator<SyncNode> nodeIterator;
        private SyncNode currentDataNode;

        public DataIterator(Iterator<SyncNode> nodeIterator) {
            this.nodeIterator = nodeIterator;
        }

        public void remove() {
            nodeIterator.remove();
        }

        public boolean hasNext() {
            this.currentDataNode = null;
            while (nodeIterator.hasNext()) {
                SyncNode node = nodeIterator.next();
                if (Node_Type_Data == node.getType()) {
                    currentDataNode = node;
                    return true;
                }
            }
            return false;
        }

        public E next() {
            return currentDataNode != null ? (E) (currentDataNode.getValue()) : null;
        }
    }
}
