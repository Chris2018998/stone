/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import static org.jmin.stone.synchronizer.impl.ThreadNodeState.WAITING;

/**
 * Wait chain node
 *
 * @author Chris Liao
 * @version 1.0
 */

class ThreadNode {
    private Thread thread;
    private volatile int state;
    private volatile Object value;

    //chain info
    private volatile int emptyInd;
    private volatile ThreadNode prev;
    private volatile ThreadNode next;

    ThreadNode() {
        this.state = WAITING;//default state
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

    public int getState() {
        return this.state;
    }

    public void setState(int newState) {
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
