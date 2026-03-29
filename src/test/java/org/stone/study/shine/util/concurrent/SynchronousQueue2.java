/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.study.shine.util.concurrent;

import jdk.internal.misc.Unsafe;
import org.stone.study.shine.util.UnsafeHolder;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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
    private static final long spinForTimeoutThreshold = 1023L;
    private static final Unsafe U = UnsafeHolder.getUnsafe();

    private static final int actionType_Get = 0;
    private static final int actionType_Data = 1;
    private static final Object FailedValue = new Object();


    private final BufferMatcher<E> matcher;

    //****************************************************************************************************************//
    //                                     1: constructors(2)                                                         //
    //****************************************************************************************************************//
    public SynchronousQueue2() {
        this(false);
    }

    public SynchronousQueue2(boolean fair) {
        // this.matcher = fair ? new QueueMatcher<E>() : new StackMatcher<E>();
        this.matcher = new QueueMatcher();
    }

    //****************************************************************************************************************//
    //                                     2: offer/put methods(3)                                                    //
    //****************************************************************************************************************//
    public boolean offer(E e) {
//        if (e == null) throw new NullPointerException();
//        return matcher.tryMatch(new Node<>(actionType_Data, e)) != null;
        return false;
    }

    public void put(E e) throws InterruptedException {
//        if (e == null) throw new NullPointerException();
//        if (Thread.interrupted()) throw new InterruptedException();
//
//        E matchedItem = matcher.match(new Node<>(actionType_Data, e), 0);
//
//        boolean isInterrupted = Thread.interrupted();//clear interrupted status
//        if (matchedItem == null && isInterrupted) throw new InterruptedException();
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
//        if (e == null) throw new NullPointerException();
//        if (Thread.interrupted()) throw new InterruptedException();
//
//        E matchedItem = matcher.match(new Node<>(actionType_Data, e), unit.toNanos(timeout));
//
//        boolean isInterrupted = Thread.interrupted();//clear interrupted status
//        if (matchedItem == null && isInterrupted) throw new InterruptedException();
//        return matchedItem != null;
        return false;
    }

    //****************************************************************************************************************//
    //                                          3: poll/take methods(3)                                               //
    //****************************************************************************************************************//
    public E poll() {
        //return (E) matcher.tryMatch(null);
        return null;
    }

    public E take() throws InterruptedException {
        return null;
//        if (Thread.interrupted()) throw new InterruptedException();
//
//        E matchedItem = matcher.match(new Node<E>(actionType_Get, null), 0);
//
//        boolean isInterrupted = Thread.interrupted();//clear interrupted status
//        if (matchedItem == null && isInterrupted) throw new InterruptedException();
//        return matchedItem;
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
//        if (Thread.interrupted()) throw new InterruptedException();
//
//        E matchedItem = matcher.match(new Node<E>(actionType_Get, null), unit.toNanos(timeout));
//
//        boolean isInterrupted = Thread.interrupted();//clear interrupted status
//        if (matchedItem == null && isInterrupted) throw new InterruptedException();
//        return matchedItem;
    }

    //****************************************************************************************************************//
    //                                      4: queue other methods                                                    //
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

    //****************************************************************************************************************//
    //                                      5: Wait node                                                              //
    //****************************************************************************************************************//
    private static final class Node<E> {
        private static final long nextOffset;
        private static final long matchedOffset;

        static {
            try {
                Class<?> k = Node.class;
                nextOffset = U.objectFieldOffset(k, "next");
                matchedOffset = U.objectFieldOffset(k, "matched");
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        private final int type;
        //node value
        private final E value;
        //node thread
        private final Thread waiter;
        //matched object
        private volatile Object matched;

        //chain node
        private volatile Node<E> next;

        Node(int type, E value) {
            this.type = type;
            this.value = value;
            this.waiter = Thread.currentThread();
        }

        private boolean isSameType(int type) {
            return this.type == type;
        }

        private boolean isMatchType(int matchType) {
            return matchType == type;
        }

        private boolean casMatch(Object match) {
            return U.compareAndSetReference(this, matchedOffset, null, match);
        }

        private void casNext(Node<E> cmp, Node<E> newNode) {
            if (next == cmp) U.compareAndSetReference(this, nextOffset, cmp, newNode);
        }
    }

    //****************************************************************************************************************//
    //                                      5: BufferMatcher Interface                                                //
    //****************************************************************************************************************//
    private static abstract class BufferMatcher<E> {
        private static final long headOffset;
        private static final long tailOffset;

        static {
            try {
                Class<?> k = BufferMatcher.class;
                headOffset = U.objectFieldOffset(k, "head");
                tailOffset = U.objectFieldOffset(k, "tail");
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        protected transient volatile Node<E> head;
        protected transient volatile Node<E> tail;

        BufferMatcher() {
            //head is fixed node(hold in chain)
            this.head = new Node<>(actionType_Data, null);
            this.tail = head;
        }

        abstract Object tryMatch(E e, int matchType);

        abstract Object match(E e, int type, int matchType, long timeoutNanos);

//        boolean casHead(Node<E> oldHead, Node<E> newHead) {
//            return oldHead == head && U.compareAndSetReference(this, headOffset, oldHead, newHead);
//        }

        boolean casTail(Node<E> oldTail, Node<E> newTail) {
            return oldTail == tail && U.compareAndSetReference(this, tailOffset, oldTail, newTail);
        }
    }

    //****************************************************************************************************************//
    //                                      6: Matcher Impl By Queue                                                  //
    //****************************************************************************************************************//
    private static final class QueueMatcher<E> extends BufferMatcher<E> {
        public Object tryMatch(E e, final int matchType) {
            Node<E> prev = head;
            Node<E> currentNode = prev.next;

            while (currentNode != null && currentNode.type == matchType) {
                if (currentNode.matched == null && currentNode.casMatch(e)) {
                    LockSupport.unpark(currentNode.waiter);

                    //remove from chain
                    prev.casNext(currentNode, currentNode.next);
                    return currentNode.value;
                }

                prev = currentNode;
                currentNode = prev.next;
            }

            return FailedValue;//need return a dummy value
        }

        public Object match(E e, final int type, final int matchType, long timeoutNanos) {
            Object matchValue = tryMatch(e, matchType);
            if (matchValue != FailedValue) return null;

            if (timeoutNanos > 0L) {//need wait in queue chain
                Node<E> node = new Node<>(type, e);
                Node<E> prev = tail;
                prev.casNext(prev.next, node);
                casTail(tail, node);
                final long deadline = System.nanoTime() + timeoutNanos;

                for (; ; ) {
                    long parkNanos = deadline - System.nanoTime();
                    if (parkNanos > 0L) {
                        LockSupport.parkNanos(parkNanos);


                    } else {//timeout

                        return FailedValue;//need return a dummy value
                    }
                }
            }

            return null;
        }
    }


//    //****************************************************************************************************************//
//    //                                      7: Matcher Impl By Stack                                                  //
//    //****************************************************************************************************************//
//    private static final class StackMatcher<E> extends BufferMatcher<E> {
//
//        public E tryMatch(Node<E> node) {
//            return null;
//        }
//
//        //******************************* 7.2: match *****************************************************************//
//        public E match(Node<E> node, long timeoutNanos) {
//            return null;
//        }
//
//        //******************************* 7.4: Wait for being matched ************************************************//
//        private Node<E> awaitFulfill(Node<E> node, long timeout) {
//            boolean isFailed = false;//interrupted or timeout,cancel node by self
//            Thread currentThread = node.waiter;
//            boolean timed = timeout > 0L;
//            long deadline = timed ? System.nanoTime() + timeout : 0L;
//            int spinCount = head.next == node ? (timed ? maxTimedSpins : maxUntimedSpins) : 0;//spin on head node
//
//            do {
//                //1: read match node
//                Node<E> matched = node.match;
//                if (matched != null) return matched;
//
//                //2: cancel node when failed
//                if (isFailed) {
//                    node.casMatch(node);
//                } else if (spinCount > 0) {
//                    if (head.next == node)
//                        spinCount--;
//                    else
//                        spinCount = 0;
//                } else if (timed) {//4:time parking
//                    final long parkTime = deadline - System.nanoTime();
//                    if (parkTime > spinForTimeoutThreshold) {
//                        LockSupport.parkNanos(this, parkTime);
//                        isFailed = currentThread.isInterrupted();
//                    } else if (parkTime <= 0) {
//                        isFailed = true;
//                    }
//                } else {//5: parking without time
//                    LockSupport.park(this);
//                    isFailed = currentThread.isInterrupted();
//                }
//            } while (true);
//        }
//    }
}
