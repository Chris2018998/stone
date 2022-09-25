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

class ThreadNode {
    //hold count for lock
    protected int holdCount;
    //acquiring thread
    private Thread thread;
    private volatile int state;

    private volatile ThreadNode prev;
    private volatile ThreadNode next;

    ThreadNode(int state) {
        this.state = state;
        this.thread = Thread.currentThread();
    }

    public Thread getThread() {
        return thread;
    }

    protected int getHoldCount() {
        return holdCount;
    }

    protected void setHoldCount(int holdCount) {
        this.holdCount = holdCount;
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
