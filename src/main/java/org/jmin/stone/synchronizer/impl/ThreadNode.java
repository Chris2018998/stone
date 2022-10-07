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
    private static final Object DummyValue = new Object();
    private Thread thread;
    //state value @see{@link ThreadNodeState}
    private volatile int state;
    //node value
    private volatile Object value;
    //prev node of this node
    private volatile ThreadNode prev;
    //next node of this node
    private volatile ThreadNode next;

    ThreadNode() {
        this(DummyValue);
    }

    ThreadNode(Object value) {
        this.value = value;
        this.state = WAITING;//default state
        this.thread = Thread.currentThread();
    }

    public Object getValue() {
        return value;
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
