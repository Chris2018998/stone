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
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import static org.stone.shine.util.concurrent.synchronizer.base.TransferWaitPool.Node_Type_Data;
import static org.stone.shine.util.concurrent.synchronizer.base.TransferWaitPool.Node_Type_Poll;

/**
 * LinkedTransferQueue implementation by wait Pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public class LinkedTransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E>, java.io.Serializable {

    //transfer wait pool
    private final TransferWaitPool<E> waitPool;

    public LinkedTransferQueue() {
        this(null);
    }

    public LinkedTransferQueue(Collection<? extends E> c) {
        this.waitPool = new TransferWaitPool<>();
        if (c != null) this.addAll(c);
    }

    //****************************************************************************************************************//
    //                                     1: offer methods(4)                                                        //
    //****************************************************************************************************************//

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never park.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void put(E e) {
        offer(e);
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        return this.waitPool.offer(new SyncNode<E>(Node_Type_Data ,e))!=null;
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never park or
     * return {@code false}.
     *
     * @return {@code true} (as specified by
     * {@link java.util.concurrent.BlockingQueue#offer(Object, long, TimeUnit)
     * BlockingQueue.offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        if (e == null) throw new NullPointerException();
        SyncVisitConfig config = new SyncVisitConfig(timeout, unit);
        config.setWakeupOneOnFailure(false);
        config.allowInterruption(false);
        try {
            return this.waitPool.offer(config) != null;
        }catch(Exception ee){
            return false;
        }
    }

    //****************************************************************************************************************//
    //                                     2: transfer methods(3)                                                     //
    //****************************************************************************************************************//

    /**
     * Transfers the element to a waiting consumer immediately, if possible.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * otherwise returning {@code false} without enqueuing the element.
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean tryTransfer(E e) {
        if (e == null) throw new NullPointerException();
        SyncNode node= new SyncNode<E>(Node_Type_Data ,e);
        return this.waitPool.tryTransfer(node,Node_Type_Poll)!=null;
    }

    /**
     * Transfers the element to a consumer, waiting if necessary to do so.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * else inserts the specified element at the tail of this queue
     * and waits until the element is received by a consumer.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void transfer(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        SyncVisitConfig<E> config = new SyncVisitConfig();
        config.setNodeInitInfo(Node_Type_Data,e);
        config.setWakeupOneOnFailure(false);
        this.waitPool.transfer(null,config,Node_Type_Poll);
    }

    /**
     * Transfers the element to a consumer if it is possible to do so
     * before the timeout elapses.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * else inserts the specified element at the tail of this queue
     * and waits until the element is received by a consumer,
     * returning {@code false} if the specified wait time elapses
     * before the element can be transferred.
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        SyncVisitConfig<E> config = new SyncVisitConfig<>(timeout,unit);
        config.setNodeInitInfo(Node_Type_Data,e);
        config.setWakeupOneOnFailure(false);
        return this.waitPool.transfer(null,config,Node_Type_Poll)!=null;
    }

    //****************************************************************************************************************//
    //                                          3: poll methods(3)                                                    //
    //****************************************************************************************************************//

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * for another thread to insert it.
     *
     * @return the head of this queue
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        SyncVisitConfig<E> config = new SyncVisitConfig<>();
        config.setNodeType(Node_Type_Poll);
        config.setWakeupOneOnFailure(false);
        SyncNode<E> pairNdoe=this.waitPool.poll(config);
        return pairNdoe!=null?pairNdoe.getValue():null;
    }

    /**
     * Retrieves and removes the head of this queue, if another thread
     * is currently making an element available.
     *
     * @return the head of this queue, or {@code null} if no
     * element is available
     */
    public E poll() {
        SyncNode<E> pairNdoe=this.waitPool.poll();
        return pairNdoe!=null?pairNdoe.getValue():null;
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
        SyncVisitConfig<E> config = new SyncVisitConfig<>(timeout,unit);
        config.setNodeType(Node_Type_Poll);
        config.setWakeupOneOnFailure(false);
        SyncNode<E> pairNdoe=this.waitPool.poll(config);
        return pairNdoe!=null?pairNdoe.getValue():null;
    }


    //****************************************************************************************************************//
    //                                          4: monitor methods(8)                                                 //
    //****************************************************************************************************************//

    public E peek() {
        return waitPool.peek();
    }

    /**
     * Returns {@code true} if this queue contains no elements.
     *
     * @return {@code true} if this queue contains no elements
     */
    public boolean isEmpty() {
        return waitPool.isEmpty();
    }

    /**
     * Returns {@code true} if this queue contains waiting consumers
     *
     * @return {@code true} if this queue contains waiting consumers
     */
    public boolean hasWaitingConsumer() {
        return waitPool.hasWaitingConsumer();
    }

    /**
     * Returns {@code int} return the count of waiting consumers
     *
     * @return {@code int} return the count of waiting consumers
     */
    public int getWaitingConsumerCount() {
        return waitPool.getWaitingConsumerCount();
    }

    /**
     * Returns the number of elements in this queue.  If this queue
     * contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these queues, determining the current
     * number of elements requires an O(n) traversal.
     *
     * @return the number of elements in this queue
     */
    public int size() {
        return waitPool.size();
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.
     * Returns {@code true} if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        return waitPool.remove(o);
    }

    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     */
    public boolean contains(Object o) {
        return waitPool.contains(o);
    }

    /**
     * Always returns {@code Integer.MAX_VALUE} because a
     * {@code LinkedTransferQueue} is not capacity constrained.
     *
     * @return {@code Integer.MAX_VALUE} (as specified by
     * {@link java.util.concurrent.BlockingQueue#remainingCapacity()
     * BlockingQueue.remainingCapacity})
     */
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }


    //****************************************************************************************************************//
    //                                          5: iterator methods(3)                                                //
    //****************************************************************************************************************//

    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
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

    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
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

    /**
     * Returns an iterator over the elements in this queue in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    public Iterator<E> iterator() {
        return waitPool.iterator();
    }
}
