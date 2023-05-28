/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * SynchronousQueue implementation with node chain(similar to JDK)
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronousQueue2<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
    private static final int NCPUS = Runtime.getRuntime().availableProcessors();
    private static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;
    private static final int maxUntimedSpins = maxTimedSpins * 16;
    private static final long spinForTimeoutThreshold = 1000L;
    private Transferer<E> transfer;

    public SynchronousQueue2() {
        this(false);
    }

    public SynchronousQueue2(boolean fair) {
        //this.transfer = fair ? new TransferQueue() : new TransferStack();
        //@todo
    }

    //****************************************************************************************************************//
    //                                     1: transfer methods                                                        //
    //****************************************************************************************************************//
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        Object re = transfer.transfer(e, false, 0);
        Thread.interrupted();
        return re != null;
    }

    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        transfer.transfer(e, true, 0);
        if (Thread.interrupted()) throw new InterruptedException();
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        Object re = transfer.transfer(e, true, unit.toNanos(timeout));
        if (re == null && Thread.interrupted()) throw new InterruptedException();
        return re != null;
    }

    //****************************************************************************************************************//
    //                                          2: get methods                                                        //
    //****************************************************************************************************************//
    public E poll() {
        Object re = transfer.transfer(null, false, 0);
        Thread.interrupted();
        return (E) re;
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        Object re = transfer.transfer(null, true, unit.toNanos(timeout));
        if (re == null && Thread.interrupted()) throw new InterruptedException();
        return (E) re;
    }

    public E take() throws InterruptedException {
        Object re = transfer.transfer(null, true, 0);
        if (re == null && Thread.interrupted()) throw new InterruptedException();
        return (E) re;
    }

    //****************************************************************************************************************//
    //                                      2:queue other methods                                                     //
    //****************************************************************************************************************//
    public boolean isEmpty() {
        return true;
    }

    public int size() {
        return 0;
    }

    public int remainingCapacity() {
        return 0;
    }

    public void clear() {
    }

    public boolean contains(Object o) {
        return false;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    public boolean removeAll(Collection<?> c) {
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        return false;
    }

    public E peek() {
        return null;
    }

    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    public Object[] toArray() {
        return new Object[0];
    }

    public <T> T[] toArray(T[] a) {
        if (a.length > 0)
            a[0] = null;
        return a;
    }

    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; (e = poll()) != null; ) {
            c.add(e);
            ++n;
        }
        return n;
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; n < maxElements && (e = poll()) != null; ) {
            c.add(e);
            ++n;
        }
        return n;
    }

    //copy from JDK
    private interface Transferer<E> {
        E transfer(E e, boolean timed, long nanos);
    }
}
