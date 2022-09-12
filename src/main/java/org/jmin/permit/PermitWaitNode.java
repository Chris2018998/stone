/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.permit;

/**
 * @author Chris Liao
 * @version 1.0
 */

public final class PermitWaitNode {
    private Thread thread;
    private volatile int state;
    private volatile PermitWaitNode prev;
    private volatile PermitWaitNode next;

    public PermitWaitNode(int state) {
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

    public PermitWaitNode getPrev() {
        return prev;
    }

    public void setPrev(PermitWaitNode prev) {
        this.prev = prev;
    }

    public PermitWaitNode getNext() {
        return next;
    }

    public void setNext(PermitWaitNode next) {
        this.next = next;
    }
}
