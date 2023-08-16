/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent;

import org.stone.shine.util.concurrent.synchronizer.SyncNode;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.base.TransferWaitPool;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.stone.shine.util.concurrent.synchronizer.base.TransferWaitPool.Node_Type_Data;
import static org.stone.shine.util.concurrent.synchronizer.base.TransferWaitPool.Node_Type_Get;

/**
 * SynchronousQueue implementation by wait Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    //transfer wait pool
    private final TransferWaitPool<E> waitPool;

    /**
     * Creates a {@code SynchronousQueue} with non-fair access policy.
     */
    public SynchronousQueue() {
        this(false);
    }

    /**
     * Creates a {@code SynchronousQueue} with the specified fairness policy.
     *
     * @param fair if true, waiting runnable contend in FIFO order for
     *             access; otherwise the order is unspecified.
     */
    public SynchronousQueue(boolean fair) {
        this.waitPool = new TransferWaitPool<>(fair);
    }

    //****************************************************************************************************************//
    //                                     1: transfer methods                                                        //
    //****************************************************************************************************************//

    /**
     * Inserts the specified element into this queue, if another thread is
     * waiting to receive it.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this queue, else
     * {@code false}
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        return this.waitPool.offer(new SyncNode<E>(null, Node_Type_Data, e)) != null;
    }

    /**
     * Adds the specified element to this queue, waiting if necessary for
     * another thread to receive it.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        SyncVisitConfig<E> config = new SyncVisitConfig<E>();
        config.setNodeInitInfo(Node_Type_Data, e);
        config.setWakeupOneOnFailure(false);
        this.waitPool.transfer(config, Node_Type_Get);
    }

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * up to the specified wait time for another thread to receive it.
     *
     * @return {@code true} if successful, or {@code false} if the
     * specified waiting time elapses before a consumer appears
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        SyncVisitConfig<E> config = new SyncVisitConfig<E>(timeout, unit);
        config.setNodeInitInfo(Node_Type_Data, e);
        config.setWakeupOneOnFailure(false);
        return this.waitPool.transfer(config, Node_Type_Get) != null;
    }

    //****************************************************************************************************************//
    //                                          2: get methods                                                        //
    //****************************************************************************************************************//

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * for another thread to insert it.
     *
     * @return the head of this queue
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        SyncVisitConfig<E> config = new SyncVisitConfig<E>();
        config.setNodeType(Node_Type_Get);
        config.setWakeupOneOnFailure(false);
        SyncNode<E> pairNode = this.waitPool.poll(config);
        return pairNode != null ? pairNode.getValue() : null;
    }

    /**
     * Retrieves and removes the head of this queue, waiting
     * if necessary up to the specified wait time, for another thread
     * to insert it.
     *
     * @return the head of this queue, or {@code null} if the
     * specified waiting time elapses before an element is present
     * @throws InterruptedException {@inheritDoc}
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        SyncVisitConfig<E> config = new SyncVisitConfig<E>(timeout, unit);
        config.setNodeType(Node_Type_Get);
        config.setWakeupOneOnFailure(false);
        SyncNode<E> pairNode = this.waitPool.poll(config);
        return pairNode != null ? pairNode.getValue() : null;
    }

    /**
     * Retrieves and removes the head of this queue, if another thread
     * is currently making an element available.
     *
     * @return the head of this queue, or {@code null} if no
     * element is available
     */
    public E poll() {
        SyncNode<E> pairNode = this.waitPool.poll();
        return pairNode != null ? pairNode.getValue() : null;
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
}
