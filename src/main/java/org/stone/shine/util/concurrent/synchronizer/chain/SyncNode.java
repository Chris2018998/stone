/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer.chain;

/**
 * Synchronized node
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class SyncNode<E> {
    Thread thread;//node thread
    volatile Object state;//node state

    //chain info(unusable fields at present)
    volatile SyncNode prev;
    volatile SyncNode next;
    private Object type;//node type
    private E value;//node value

    //****************************************************************************************************************//
    //                                                1: constructor(2)                                               //
    //****************************************************************************************************************//
    //used in queues(ConcurrentLinkedQueue,ConcurrentLinkedDeque)
    public SyncNode(Object state) {
        this.state = state;
    }

    //used in wait-pools
    public SyncNode(Object type, E value) {
        this.type = type;
        this.value = value;
    }

    //****************************************************************************************************************//
    //                                           set/get of chain node                                                //
    //****************************************************************************************************************//
    public E getValue() {
        return this.value;
    }

    public Object getType() {
        return this.type;
    }

    public void setType(Object type) {
        this.type = type;
    }

    public Object getState() {
        return this.state;
    }

    public void setState(Object newState) {
        this.state = newState;
    }

    public Thread getThread() {
        return this.thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public boolean receivedSignal() {
        if (this.state == SyncNodeStates.RUNNING) {
            this.state = null;//rest to null for next
            return true;
        }
        return false;
    }

    //****************************************************************************************************************//
    //                                          unusable fields                                                       //
    //****************************************************************************************************************//
    public SyncNode getPrev() {
        return this.prev;
    }

    public SyncNode setPrev(SyncNode prev) {
        return this.prev = prev;
    }

    public SyncNode getNext() {
        return this.next;
    }

    public SyncNode setNext(SyncNode next) {
        return this.next = next;
    }

    public String toString() {
        return thread + ":" + type + ":" + state + ":" + value;
    }
}
