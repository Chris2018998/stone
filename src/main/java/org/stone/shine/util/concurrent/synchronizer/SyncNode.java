/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

/**
 * Synchronized node
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class SyncNode<E> {
    Object type;//node type
    Thread thread;//node thread
    volatile Object state;//node state

    //chain info(unusable fields at present)
    volatile SyncNode prev;
    volatile SyncNode next;
    private E value;//node value

    //****************************************************************************************************************//
    //                                                1: constructor(3)                                               //
    //****************************************************************************************************************//
    public SyncNode(Object state) {
        this.state = state;
    }

    public SyncNode(Object state, E value) {
        this.state = state;
        this.value = value;
    }

    public SyncNode(Object state, Object type, E value) {
        this.state = state;
        this.type = type;
        this.value = value;
    }

    //****************************************************************************************************************//
    //                                           set/get of chain node                                                //
    //****************************************************************************************************************//
    public final E getValue() {
        return value;
    }

    public final Object getType() {
        return type;
    }

    public final void setType(Object type) {
        this.type = type;
    }

    public final Object getState() {
        return state;
    }

    public final void setState(Object newState) {
        this.state = newState;
    }

    public final Thread getOwnerThread() {
        return this.thread;
    }

    final void setOwnerThread() {
        this.thread = Thread.currentThread();
    }

    //****************************************************************************************************************//
    //                                          unusable fields                                                       //
    //****************************************************************************************************************//
    public SyncNode getPrev() {
        return prev;
    }

    public SyncNode setPrev(SyncNode prev) {
        return this.prev = prev;
    }

    public SyncNode getNext() {
        return next;
    }

    public SyncNode setNext(SyncNode next) {
        return this.next = next;
    }
}
