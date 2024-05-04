/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.study;

import org.stone.tools.unsafe.UnsafeAdaptor;
import org.stone.tools.unsafe.UnsafeAdaptorHolder;

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
    private static final UnsafeAdaptor U = UnsafeAdaptorHolder.U;

    private final BufferMatcher<E> matcher;

    //****************************************************************************************************************//
    //                                     1: constructors(2)                                                         //
    //****************************************************************************************************************//
    public SynchronousQueue2() {
        this(false);
    }

    public SynchronousQueue2(boolean fair) {
        this.matcher = fair ? new QueueMatcher<E>() : new StackMatcher<E>();
    }

    //****************************************************************************************************************//
    //                                     2: offer/put methods(3)                                                    //
    //****************************************************************************************************************//
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        return matcher.tryMatch(new Node<>(e)) != null;
    }

    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (Thread.interrupted()) throw new InterruptedException();

        E matchedItem = matcher.match(new Node<>(e), 0);

        boolean isInterrupted = Thread.interrupted();//clear interrupted status
        if (matchedItem == null && isInterrupted) throw new InterruptedException();
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (Thread.interrupted()) throw new InterruptedException();

        E matchedItem = matcher.match(new Node<>(e), unit.toNanos(timeout));

        boolean isInterrupted = Thread.interrupted();//clear interrupted status
        if (matchedItem == null && isInterrupted) throw new InterruptedException();
        return matchedItem != null;
    }

    //****************************************************************************************************************//
    //                                          3: poll/take methods(3)                                               //
    //****************************************************************************************************************//
    public E poll() {
        return matcher.tryMatch(new Node<E>(null));
    }

    public E take() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();

        E matchedItem = matcher.match(new Node<E>(null), 0);

        boolean isInterrupted = Thread.interrupted();//clear interrupted status
        if (matchedItem == null && isInterrupted) throw new InterruptedException();
        return matchedItem;
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();

        E matchedItem = matcher.match(new Node<E>(null), unit.toNanos(timeout));

        boolean isInterrupted = Thread.interrupted();//clear interrupted status
        if (matchedItem == null && isInterrupted) throw new InterruptedException();
        return matchedItem;
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
    //                                      5: BufferMatcher Interface                                                //
    //****************************************************************************************************************//
    private static abstract class BufferMatcher<E> {
        private static final long headOffset;
        private static final long tailOffset;

        static {
            try {
                Class<?> k = BufferMatcher.class;
                headOffset = U.objectFieldOffset
                        (k.getDeclaredField("head"));
                tailOffset = U.objectFieldOffset
                        (k.getDeclaredField("tail"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        protected transient volatile Node<E> head;
        protected transient volatile Node<E> tail;

        BufferMatcher() {
            this.head = new Node<>(null);
            this.tail = head;
        }

        //******************************* 5.2: abstract methods(2)****************************************************//
        abstract E tryMatch(Node<E> e);

        abstract E match(Node<E> e, long timeoutNanos);

        //******************************* 5.1: Chain Cas(2)***********************************************************//
        boolean casHead(Node oldHead, Node newHead) {
            return oldHead == head && U.compareAndSwapObject(this, headOffset, oldHead, newHead);
        }

        void casTail(Node oldTail, Node newTail) {
            if (oldTail == tail) U.compareAndSwapObject(this, tailOffset, oldTail, newTail);
        }
    }

    //****************************************************************************************************************//
    //                                      6: Matcher Impl By Queue                                                  //
    //****************************************************************************************************************//
    private static final class QueueMatcher<E> extends BufferMatcher<E> {

        public E tryMatch(Node<E> s) {
            Node<E> h = head;
            Node<E> current = h.next;
            final boolean isData = s.isData;
            if (current == null || current.isData == isData) return null;

            E matchedItem = null;
            do {
                if (current.match == null && current.casMatch(s)) {
                    LockSupport.unpark(current.waiter);
                    matchedItem = isData ? s.item : current.item;
                    break;
                }

                Node<E> nextNode = current.next;
                if (nextNode == null || nextNode.isData == isData) break;
                current = nextNode;
            } while (true);

            casHead(h, current);
            return matchedItem;
        }

        //******************************* 6.2: match *****************************************************************//
        public E match(Node<E> s, long nanos) {
            final boolean isData = s.isData;
            E matchedItem;

            do {
                Node t = tail;
                Node h = head;
                if (h == t || t.isData == isData) { // empty or same-mode
                    if (!t.casNext(null, s)) continue;
                    casTail(t, s);

                    Node<E> x = awaitFulfill(s, nanos);
                    if (x == s) {//cancelled
                        Node newNext = s.next;
                        if (newNext != null) t.casNext(s, newNext);//s at middle
                        return null;
                    }

                    casHead(h, s);
                    return isData ? s.item : x.item;
                } else if ((matchedItem = this.tryMatch(s)) != null) {
                    return matchedItem;
                }
            } while (true);
        }

        //******************************* 6.4: Wait for being matched ************************************************//
        private Node<E> awaitFulfill(Node<E> s, long nanos) {
            boolean failed = false;
            final boolean timed = nanos > 0;
            final Thread waitThread = s.waiter;
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            int spins = head.next == s ? (timed ? maxTimedSpins : maxUntimedSpins) : 0;

            do {
                Node<E> x = s.match;
                if (x != null) return x;

                if (failed) {
                    s.casMatch(s);
                } else if (spins > 0) {
                    --spins;
                } else if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        failed = true;
                    } else if (nanos > spinForTimeoutThreshold) {
                        LockSupport.parkNanos(this, nanos);
                        failed = waitThread.isInterrupted();
                    }
                } else {
                    LockSupport.park(this);
                    failed = waitThread.isInterrupted();
                }
            } while (true);
        }
    }

    //****************************************************************************************************************//
    //                                      7: Matcher Impl By Stack                                                  //
    //****************************************************************************************************************//
    private static final class StackMatcher<E> extends BufferMatcher<E> {

        public E tryMatch(Node<E> node) {
            return null;
        }

        //******************************* 7.2: match *****************************************************************//
        public E match(Node<E> node, long timeoutNanos) {
            return null;
        }

        //******************************* 7.4: Wait for being matched ************************************************//
        private Node<E> awaitFulfill(Node<E> node, long timeout) {
            boolean isFailed = false;//interrupted or timeout,cancel node by self
            Thread currentThread = node.waiter;
            boolean timed = timeout > 0L;
            long deadline = timed ? System.nanoTime() + timeout : 0L;
            int spinCount = head.next == node ? (timed ? maxTimedSpins : maxUntimedSpins) : 0;//spin on head node

            do {
                //1: read match node
                Node<E> matched = node.match;
                if (matched != null) return matched;

                //2: cancel node when failed
                if (isFailed) {
                    node.casMatch(node);
                } else if (spinCount > 0) {
                    if (head.next == node)
                        spinCount--;
                    else
                        spinCount = 0;
                } else if (timed) {//4:time parking
                    final long parkTime = deadline - System.nanoTime();
                    if (parkTime > spinForTimeoutThreshold) {
                        LockSupport.parkNanos(this, parkTime);
                        isFailed = currentThread.isInterrupted();
                    } else if (parkTime <= 0) {
                        isFailed = true;
                    }
                } else {//5: parking without time
                    LockSupport.park(this);
                    isFailed = currentThread.isInterrupted();
                }
            } while (true);
        }
    }

    //****************************************************************************************************************//
    //                                      8: Wait node                                                              //
    //****************************************************************************************************************//
    private static final class Node<E> {
        private static final long nextOffset;
        private static final long matchedOffset;

        static {
            try {
                Class<?> k = Node.class;
                nextOffset = U.objectFieldOffset
                        (k.getDeclaredField("next"));
                matchedOffset = U.objectFieldOffset
                        (k.getDeclaredField("match"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        private final E item;
        private final boolean isData;
        private final Thread waiter;
        private volatile Node<E> next;
        private volatile Node<E> match;

        Node(E item) {
            this.item = item;
            this.isData = item != null;
            this.waiter = Thread.currentThread();
        }

        private boolean casNext(Node cmp, Node val) {
            return next == cmp && U.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        private boolean casMatch(Node val) {
            return match == null && U.compareAndSwapObject(this, matchedOffset, null, val);
        }
    }
}
