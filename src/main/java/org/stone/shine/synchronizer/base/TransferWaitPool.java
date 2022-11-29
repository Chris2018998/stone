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

import java.util.Iterator;

/**
 * transfer wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TransferWaitPool<E> extends ThreadWaitPool {
    //Request
    private static final Object Node_Type_Get = new Object();
    //Data
    private static final Object Node_Type_Data = new Object();

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

    public final boolean isFair() {
        return this.fair;
    }

    //****************************************************************************************************************//
    //                                          1: offer methods(3)                                                   //
    //****************************************************************************************************************//
    public final boolean offer(E e) {
        if (tryTransfer(e)) return true;

        this.appendDataNode(Node_Type_Data, e);
        return false;
    }

    public final boolean offer(E e, ThreadWaitConfig config) {
        try {
            if (transfer(e, config)) return true;
        } catch (InterruptedException ex) {
            //do nothing
        }

        this.appendDataNode(Node_Type_Data, e);
        return false;
    }

    //****************************************************************************************************************//
    //                                          2:transfer methods                                                    //
    //****************************************************************************************************************//
    public final boolean tryTransfer(E e) {
        if (e == null) throw new NullPointerException();
        return this.wakeupOne(fair, e, Node_Type_Get) == 1;
    }

    //transfer a object to waiter
    public final boolean transfer(E e, ThreadWaitConfig config) throws InterruptedException {
        if (e == null) throw new NullPointerException();

        //step1: try to transfer
        if (this.tryTransfer(e)) return true;

        //step2:create wait node(then to wait)
        config.setNodeValue(Node_Type_Data, e);

        //step3:create wait node(then to wait)
        return doWait(config) != null;
    }

    //****************************************************************************************************************//
    //                                          3: get methods                                                        //
    //****************************************************************************************************************//
    public final E tryGet() {
        ThreadNode node = this.getWokenUpNode(fair, ThreadNodeState.SIGNAL, Node_Type_Data);
        return node != null ? (E) node.getValue() : null;
    }

    public final E get(ThreadWaitConfig config) throws InterruptedException {
        //step1: try to get
        E e = tryGet();
        if (e != null) return e;

        //step2:create wait node(then to wait)
        config.setNodeValue(Node_Type_Get, null);

        //step3:create wait node(then to wait)
        return (E) doWait(config);
    }

    //****************************************************************************************************************//
    //                                          4: core methods                                                       //
    //****************************************************************************************************************//
    private Object doWait(ThreadWaitConfig config) throws InterruptedException {
        if (config == null) throw new IllegalArgumentException("wait config can't be null");

        //1:create wait node and offer to wait queue
        ThreadNode node = config.getThreadNode();
        if (config.isOutsideOfWaitPool()) super.appendNode(node);

        //2:get control parameters from config
        boolean throwsIE = config.isThrowsIE();
        boolean wakeupOtherOnIE = config.isTransferSignalOnIE();

        //3:create thread parker
        ThreadParkSupport parker = config.getThreadParkSupport();

        //4:spin control
        try {
            do {
                //4.1: read node state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) {//wokenUp
                    if (node.getType() == Node_Type_Data) {
                        return node;//that means transferred object has been got by other
                    } else {//state==Node_Type_Get
                        return state;
                    }
                }

                //4.2: timeout test
                if (parker.isTimeout()) {
                    //4.2.1: try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return null
                    if (ThreadNodeUpdater.casNodeState(node, null, ThreadNodeState.TIMEOUT)) return null;
                } else {
                    //4.3: park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, parker, throwsIE, wakeupOtherOnIE);
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
        Iterator<ThreadNode> nodeIterator = super.ascendingIterator();
        while (nodeIterator.hasNext()) {
            ThreadNode node = nodeIterator.next();
            if (Node_Type_Data == node.getType()) return (E) node.getValue();
        }
        return null;
    }

    public boolean remove(Object o) {
        if (o == null) return false;
        Iterator<ThreadNode> nodeIterator = super.ascendingIterator();
        while (nodeIterator.hasNext()) {
            ThreadNode node = nodeIterator.next();
            if (node.getType() == Node_Type_Data && o.equals(node.getValue())) {
                nodeIterator.remove();
                return true;
            }
        }
        return false;
    }

    public boolean contains(Object o) {
        if (o == null) return false;
        Iterator<ThreadNode> nodeIterator = super.ascendingIterator();
        while (nodeIterator.hasNext()) {
            ThreadNode node = nodeIterator.next();
            if (node.getType() == Node_Type_Data && o.equals(node.getValue())) return true;
        }
        return false;
    }

    public Iterator<E> iterator() {
        return new DataIterator<E>(super.ascendingIterator());
    }

    private static class DataIterator<E> implements Iterator {
        private final Iterator<ThreadNode> nodeIterator;
        private ThreadNode currentDataNode;

        public DataIterator(Iterator<ThreadNode> nodeIterator) {
            this.nodeIterator = nodeIterator;
        }

        public void remove() {
            nodeIterator.remove();
        }

        public boolean hasNext() {
            this.currentDataNode = null;
            while (nodeIterator.hasNext()) {
                ThreadNode node = nodeIterator.next();
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
