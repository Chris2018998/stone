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
    private final Object type;//node type
    private Thread thread;//node thread
    private volatile Object state;//cas field
    private Object value;//node value

    //chain info(unusable fields at present)
    private volatile int emptyInd;
    private volatile ThreadNode prev;
    private volatile ThreadNode next;

    //wait node
    ThreadNode(Object type) {
        this.type = type;
        this.thread = Thread.currentThread();
    }

    //data node
    ThreadNode(Object type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Thread getThread() {
        return this.thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public Object getState() {
        return this.state;
    }

    public void setState(Object newState) {
        this.state = newState;
    }

    public Object getType() {
        return this.type;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
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
