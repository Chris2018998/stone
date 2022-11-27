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
 * Wait chain node
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ThreadNode {
    private Object type;//node type
    private Object value;//node value
    private Thread thread;//node thread
    private volatile Object state;//cas field

    //chain info(unusable fields at present)
    private volatile int emptyInd;
    private volatile ThreadNode prev;
    private volatile ThreadNode next;

    ThreadNode(Object type, Object value, boolean needPark) {
        this.type = type;
        this.value = value;
        if (needPark) thread = Thread.currentThread();
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

    public final Object getValue() {
        return value;
    }

    public final void setValue(Object value) {
        this.value = value;
    }

    public final Thread getThread() {
        return thread;
    }

    public final void setThread(Thread thread) {
        this.thread = thread;
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
    int getEmptyInd() {
        return emptyInd;
    }

    void setEmptyInd(int emptyInd) {
        this.emptyInd = emptyInd;
    }

    public ThreadNode getPrev() {
        return prev;
    }

    public void setPrev(ThreadNode prev) {
        this.prev = prev;
    }

    public ThreadNode getNext() {
        return next;
    }

    public void setNext(ThreadNode next) {
        this.next = next;
    }
}
