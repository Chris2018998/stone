/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

/**
 * @author Chris Liao
 * @version 1.0
 */

final class SynchronizedNode {
    private Thread thread;
    private volatile int state;
    private volatile SynchronizedNode prev;
    private volatile SynchronizedNode next;

    SynchronizedNode(int state) {
        this.state = state;
        this.thread = Thread.currentThread();
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

    public SynchronizedNode getPrev() {
        return prev;
    }

    public void setPrev(SynchronizedNode prev) {
        this.prev = prev;
    }

    public SynchronizedNode getNext() {
        return next;
    }

    public void setNext(SynchronizedNode next) {
        this.next = next;
    }
}
