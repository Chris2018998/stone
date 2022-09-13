/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.concurrent.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 */

public final class WaitNode {
    private Thread thread;
    private volatile int state;
    private volatile WaitNode prev;
    private volatile WaitNode next;

    public WaitNode(int state) {
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

    public WaitNode getPrev() {
        return prev;
    }

    public void setPrev(WaitNode prev) {
        this.prev = prev;
    }

    public WaitNode getNext() {
        return next;
    }

    public void setNext(WaitNode next) {
        this.next = next;
    }
}
