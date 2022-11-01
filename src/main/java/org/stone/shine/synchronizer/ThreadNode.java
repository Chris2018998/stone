/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.synchronizer;

/**
 * Wait chain node
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ThreadNode {
    private final Thread thread;
    private volatile Object value;
    private volatile Object state;

    //chain info
    private volatile int emptyInd;
    private volatile ThreadNode prev;
    private volatile ThreadNode next;

    ThreadNode() {
        this.thread = Thread.currentThread();
    }

    ThreadNode(Object value) {
        this.value = value;
        this.thread = Thread.currentThread();
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Thread getThread() {
        return thread;
    }

    public Object getState() {
        return this.state;
    }

    public void setState(Object newState) {
        this.state = newState;
    }

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
