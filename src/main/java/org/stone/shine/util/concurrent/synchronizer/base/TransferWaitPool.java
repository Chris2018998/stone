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

/**
 * Transfer-WaitPool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TransferWaitPool<E> extends ThreadWaitingPool<E> {
    public static final Object Node_Type_Poll = new Object();
    public static final Object Node_Type_Data = new Object();

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
    //try to transfer node to a waiter,success return the wait node,failed append to queue as a node
    public final SyncNode<E> offer(SyncNode<E> n) {
        if (n == null) throw new IllegalArgumentException("element can't be null");

        SyncNode<E> pairNode = tryTransfer(n, Node_Type_Poll);
        if (pairNode != null) return pairNode;

        this.appendAsDataNode(n);//append to queue
        return null;
    }

    //try to transfer the node to a waiter during specified period
    public final SyncNode<E> offer(SyncVisitConfig<E> config) throws InterruptedException {
        if (config == null) throw new IllegalArgumentException("element can't be null");

        SyncNode<E> node = config.getSyncNode();
        SyncNode<E> pairNode = transfer(node, config, Node_Type_Poll);
        if (pairNode != null) return pairNode;

        this.appendAsDataNode(node);
        return null;
    }

    //****************************************************************************************************************//
    //                                          2: transfer methods(2)                                                //
    //****************************************************************************************************************//
    public final SyncNode<E> tryTransfer(SyncNode<E> node, Object toNodeType) {
        return this.wakeupOne(fair, toNodeType, node);
    }

    public final SyncNode<E> transfer(SyncNode<E> node, SyncVisitConfig<E> config, Object toNodeType) throws InterruptedException {
        if (node == null) node = config.getSyncNode();
        SyncNode<E> pairNode = tryTransfer(node, toNodeType);
        if (pairNode != null) return pairNode;

        return doWait(config);
    }

    //****************************************************************************************************************//
    //                                          3: poll methods(2)                                                //
    //****************************************************************************************************************//
    public final SyncNode<E> poll() {
        SyncNode<E> node = new SyncNode<>(Node_Type_Poll, null);
        return tryTransfer(node, Node_Type_Data);
    }

    public final SyncNode<E> poll(SyncVisitConfig<E> config) throws InterruptedException {
        if (config == null) throw new IllegalArgumentException("config can't be null");

        SyncNode<E> node = config.getSyncNode();
        SyncNode<E> pollNode = transfer(node, config, Node_Type_Data);
        if (pollNode != null) return pollNode;

        return doWait(config);
    }

    //****************************************************************************************************************//
    //                                          4: core methods                                                       //
    //****************************************************************************************************************//
    private SyncNode<E> doWait(SyncVisitConfig<E> config) throws InterruptedException {
        if (config == null) throw new IllegalArgumentException("wait config can't be null");
        if (Thread.interrupted()) throw new InterruptedException();

        //1:create wait node and offer to wait queue
        SyncNode node = config.getSyncNode();
        int spins = appendAsWaitNode(node) ? maxTimedSpins : 0;//spin count

        //2:get control parameters from config
        boolean allowInterrupted = config.isAllowInterruption();
        ThreadParkSupport parkSupport = config.getParkSupport();

        //3:spin control
        try {
            do {
                //3.1: read node state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) {//wokenUp
                    if (state == TIMEOUT) return null;
                    if (state == INTERRUPTED) throw new InterruptedException();
                    if (state instanceof SyncNode)
                        return (SyncNode<E>) state;
                }

                //3.3: fail check
                if (parkSupport.isTimeout()) {
                    casState(node, null, TIMEOUT);
                } else if (parkSupport.isInterrupted() && allowInterrupted) {
                    casState(node, null, INTERRUPTED);
                } else if (state != null) {
                    node.setState(null);
                } else if (spins > 0) {
                    --spins;
                } else {
                    parkSupport.tryToPark();
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
        return super.existsTypeNode(Node_Type_Poll);
    }

    public int getWaitingConsumerCount() {
        return super.getQueueLength(Node_Type_Poll);
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
