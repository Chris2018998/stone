/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer;

/**
 * common cas node class
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class CasNode<E> {
    Object type;//node type
    Thread thread;//node thread
    volatile Object state;//cas field

    //chain info(unusable fields at present)
    volatile CasNode prev;
    volatile CasNode next;

    private E value;//node value

    //****************************************************************************************************************//
    //                                                1: constructor(2)                                               //
    //****************************************************************************************************************//
    //A: data node work in outside queue or chain(for example:ConcurrentLinkedQueue,ConcurrentLinkedDeque)
    public CasNode(Object state) {
        this.state = state;
    }

    //B: thread node in syn chain/queue(for synchronizer package)
    CasNode(Object type, E value) {
        this.type = type;
        this.value = value;
        this.thread = Thread.currentThread();
    }

    CasNode(Object type, E value, Object state) {
        this.type = type;
        this.value = value;
        this.state = state;
        this.thread = Thread.currentThread();
    }


    //****************************************************************************************************************//
    //                                           set/get of chain node                                                //
    //****************************************************************************************************************//
    public final Object getType() {
        return type;
    }

    public final void setType(Object type) {
        this.type = type;
    }

    public final E getValue() {
        return value;
    }

    public final Object getState() {
        return state;
    }

    public final void setState(Object newState) {
        this.state = newState;
    }

    //****************************************************************************************************************//
    //                                          unusable fields                                                       //
    //****************************************************************************************************************//

    public CasNode getPrev() {
        return prev;
    }

    public CasNode setPrev(CasNode prev) {
        return this.prev = prev;
    }

    public CasNode getNext() {
        return next;
    }

    public CasNode setNext(CasNode next) {
        return this.next = next;
    }
}
