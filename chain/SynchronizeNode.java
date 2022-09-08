/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.chain;

import org.jmin.util.concurrent.UnsafeUtil;
import sun.misc.Unsafe;

/**
 * @author Chris Liao
 * @version 1.0
 */

public class SynchronizeNode {
    //***************************************************************************************************************//
    //                                           1: CAS Chain info                                                   //
    //***************************************************************************************************************//
    private final static Unsafe U;
    private final static long stateOffSet;
    private final static long prevOffSet;
    private final static long nextOffSet;

    static {
        try {
            U = UnsafeUtil.getUnsafe();
            prevOffSet = U.objectFieldOffset(SynchronizeNode.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(SynchronizeNode.class.getDeclaredField("next"));
            stateOffSet = U.objectFieldOffset(SynchronizeNode.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    //***************************************************************************************************************//
    //                                          2: local attributes                                                  //
    //***************************************************************************************************************//
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

    public boolean compareAndSetState(int expect, int update) {
        return U.compareAndSwapObject(this, stateOffSet, expect, update);
    }


    public SynchronizeNode getPrev() {
        return prev;
    }

    public void setPrev(SynchronizeNode prev) {
        this.prev = prev;
    }

    public boolean compareAndSetPrev(SynchronizeNode expect, SynchronizeNode update) {
        return U.compareAndSwapObject(this, prevOffSet, expect, update);
    }


    public SynchronizeNode getNext() {
        return next;
    }

    public void setNext(SynchronizeNode next) {
        this.next = next;
    }

    public boolean compareAndSetNext(SynchronizeNode expect, SynchronizeNode update) {
        return U.compareAndSwapObject(this, nextOffSet, expect, update);
    }
}
