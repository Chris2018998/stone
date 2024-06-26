/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer;

import org.stone.shine.util.concurrent.synchronizer.chain.SyncNode;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeStates.REMOVED;
import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.maxTimedSpins;

/**
 * Transfer-ObjectWaitPool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TransferWaitPool<E> extends ObjectWaitPool {
    public static final Object Node_Type_Get = new Object();
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
        super(new ConcurrentLinkedDeque<SyncNode>());
        this.fair = fair;
        //true:transfer from head,which similar to{@link java.util.concurrent.SynchronousQueue#TransferQueue}
        //false:transfer from tail,which similar to{@link java.util.concurrent.SynchronousQueue#TransferStack}
    }

    public boolean isFair() {
        return this.fair;
    }

    //****************************************************************************************************************//
    //                                          1: offer methods(3)                                                   //
    //****************************************************************************************************************//
    //try to transfer node to a waiter,success return the wait node,failed append to queue as a node
    public SyncNode offer(SyncNode node) {
        if (node == null) throw new IllegalArgumentException("Node can't be null");
        SyncNode pairNode = tryTransfer(node, Node_Type_Get);
        if (pairNode != null) return pairNode;//matched node
        this.appendAsDataNode(node);//append to queue
        return null;
    }

    //try to transfer the node to a waiter during specified period
    public SyncNode offer(SyncVisitConfig config) throws InterruptedException {
        if (config == null) throw new NullPointerException("Sync config can't be null");
        SyncNode pairNode = transfer(config, Node_Type_Get);
        if (pairNode != null) return pairNode;//matched node
        this.appendAsDataNode(config.getSyncNode());
        return null;
    }

    //****************************************************************************************************************//
    //                                          2: transfer methods(2)                                                //
    //****************************************************************************************************************//
    public SyncNode tryTransfer(SyncNode node, Object toNodeType) {
        if (node == null) throw new NullPointerException("Node can't be null");
        return this.wakeupOne(fair, toNodeType, node);
    }

    public SyncNode transfer(SyncVisitConfig config, Object toNodeType) throws InterruptedException {
        if (config == null) throw new NullPointerException("Config can't be null");

        //1: try to transfer to one waiter
        SyncNode pairNode = tryTransfer(config.getSyncNode(), toNodeType);
        if (pairNode != null) return pairNode;

        //2: wait for transferring
        return doWait(config);
    }

    //****************************************************************************************************************//
    //                                          3: poll methods(2)                                                    //
    //****************************************************************************************************************//
    public SyncNode poll() {
        return tryTransfer(new SyncNode(Node_Type_Get, null), Node_Type_Data);
    }

    public SyncNode poll(SyncVisitConfig config) throws InterruptedException {
        if (config == null) throw new NullPointerException("Sync config can't be null");

        return transfer(config, Node_Type_Data);
    }

    //****************************************************************************************************************//
    //                                          4: core methods                                                       //
    //****************************************************************************************************************//
    private SyncNode doWait(SyncVisitConfig config) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        if (config == null) throw new NullPointerException("Sync config can't be null");

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
                if (state instanceof SyncNode) //wokenUp
                    return (SyncNode) state;

                //3.3: fail check
                if (parkSupport.isTimeout()) {
                    if (casState(node, null, REMOVED)) return null;
                } else if (parkSupport.isInterrupted() && allowInterrupted) {
                    if (casState(node, null, REMOVED)) throw new InterruptedException();
                } else if (state != null) {
                    node.setState(null);
                } else if (spins > 0) {
                    --spins;
                } else {
                    parkSupport.computeAndPark();
                }
            } while (true);
        } finally {
            waitQueue.remove(node);
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

    public Iterator iterator() {
        return new DataIterator(super.ascendingIterator());
    }

    private static class DataIterator<E> implements Iterator {
        private final Iterator<SyncNode> nodeIterator;
        private SyncNode currentDataNode;

        DataIterator(Iterator<SyncNode> nodeIterator) {
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
            return currentDataNode != null ? (E) currentDataNode.getValue() : null;
        }
    }
}