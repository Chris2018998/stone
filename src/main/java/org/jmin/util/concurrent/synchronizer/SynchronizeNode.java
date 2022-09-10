/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 */

public class SynchronizeNode {
    private Thread thread;
    private volatile int state;
    private volatile SynchronizeNode prev;
    private volatile SynchronizeNode next;

    public SynchronizeNode(int state) {
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

    public SynchronizeNode getPrev() {
        return prev;
    }

    public void setPrev(SynchronizeNode prev) {
        this.prev = prev;
    }


    public SynchronizeNode getNext() {
        return next;
    }

    public void setNext(SynchronizeNode next) {
        this.next = next;
    }
}
