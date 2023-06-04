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

import sun.misc.Unsafe;

import java.lang.reflect.Field;
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
    private static final long spinForTimeoutThreshold = 1000L;
    private static final int DATA = 1;
    private static final int REQUEST = 2;
    private static final Unsafe U;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            U = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new Error("Failed to get Unsafe", e);
        }
    }


    private BufferMatcher<E> matcher;

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
        matcher.match(new Node<>(e), 0);
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (Thread.interrupted()) throw new InterruptedException();
        return matcher.match(new Node<>(e), unit.toNanos(timeout)) != null;
    }

    //****************************************************************************************************************//
    //                                          3: poll/take methods(3)                                               //
    //****************************************************************************************************************//
    public E poll() {
        return matcher.tryMatch(new Node<>((E) null));
    }

    public E take() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        return matcher.match(new Node<>((E) null), 0);
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        return matcher.match(new Node<>((E) null), unit.toNanos(timeout));
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
    private interface BufferMatcher<E> {

        E tryMatch(Node<E> e);

        E match(Node<E> e, long timeoutNanos) throws InterruptedException;
    }

    //****************************************************************************************************************//
    //                                      6: Matcher Impl By Stack                                                  //
    //****************************************************************************************************************//
    private static final class StackMatcher<E> implements BufferMatcher<E> {
        public E tryMatch(Node<E> e) {
            return null;
        }

        public E match(Node<E> e, long timeoutNanos) throws InterruptedException {
            return null;
        }
    }

    //****************************************************************************************************************//
    //                                      7: Matcher Impl By Queue                                                  //
    //****************************************************************************************************************//
    private static final class QueueMatcher<E> implements BufferMatcher<E> {
        private static final long headOffset;
        private static final long tailOffset;

        static {
            try {
                Class<?> k = QueueMatcher.class;
                headOffset = U.objectFieldOffset
                        (k.getDeclaredField("head"));
                tailOffset = U.objectFieldOffset
                        (k.getDeclaredField("tail"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        private transient volatile Node<E> head = new Node<>(null);
        private transient volatile Node<E> tail = head;

        //******************************* 7.1: Chain Cas(2)***********************************************************//
        private void casHead(Node oldHead, Node newHead) {
            if (oldHead == head) U.compareAndSwapObject(this, headOffset, oldHead, newHead);
        }

        private void casTail(Node oldTail, Node newTail) {
            if (oldTail == tail) U.compareAndSwapObject(this, tailOffset, oldTail, newTail);
        }

        //******************************* 7.2: tryMatch **************************************************************//
        public E tryMatch(Node<E> node) {
            Node<E> curNode = head.next;
            if (curNode == null) return null;

            E matchedValue = null;
            int nodeTye = node.nodeType;

            do {
                //1: exit loop when meet same type node
                if (curNode.nodeType == nodeTye) return null;

                //2: continue to try next node
                if (curNode.isMatched()) continue;

                
//                if (curNode.casMatch(node)) {//match success
//                        if (curNode.nodeType == DATA)
//                            matchedValue = curNode.item;
//                        else
//                            matchedValue = node.item;
//                        break;
//                    } else {
//                        //prev = curNode;
//                    }

            } while (true);


            return matchedValue;
        }

        //******************************* 7.3: tryMatch **************************************************************//
        public E match(Node<E> e, long timeoutNanos) throws InterruptedException {
            return null;
        }

        //******************************* 7.4: Other *****************************************************************//
        public E transfer(E e, boolean timed, long nanos) {
            Node<E> node = new Node<>(e);
            int type = node.nodeType;
            do {
                //1: try to cas match
                Node<E> h = head;
                Node<E> first = h.next;
                if (first != null) {
                    if (first.isMatched()) {
                        casHead(head, first);
                        continue;//continue to get next node,maybe it can match
                    } else if (first.nodeType != type) {
                        boolean success = first.casMatch(node);
                        casHead(head, first);//if failed,head should be moved
                        if (success) {
                            LockSupport.unpark(first.waiter);
                            if (type == REQUEST) return first.item;
                            return e;
                        }
                        continue;//maybe next still be different type
                    }
                }

                //2: return null when time not greater than zero
                if (timed && nanos <= 0) return null;

                //3: wait to
                Node<E> t = tail;
                if (head == t || t.nodeType == type) {//null or same type
                    node.prev = t;
                    node.waiter = Thread.currentThread();
                    if (t.casNext(null, node)) {
                        casTail(t, node);
                        Node<E> matched = waitForFilling(node, timed, nanos);
                        if (matched == node) return null;//cancelled
                        if (matched.nodeType == DATA) return matched.item;
                        return e;
                    }
                }
            } while (true);
        }

        //******************************* 7.5: Wait for being matched ************************************************//
        private Node<E> waitForFilling(Node<E> node, boolean timed, long nano) {
            boolean isFailed = false;//interrupted or timeout,cancel node by self
            Thread currentThread = node.waiter;
            long deadline = timed ? System.nanoTime() + nano : 0;
            int spinCount = head.next == node ? (timed ? maxTimedSpins : maxUntimedSpins) : 0;//spin on first node

            do {
                //1: read match node
                Node<E> matched = node.match;
                if (matched != null) {
                    if (node != tail) {//try to unlink from chain
                        Node prev = node.prev;
                        prev.casNext(node, node.next);
                    }
                    return matched;
                }

                //2: cancel node when failed
                if (isFailed) {
                    node.casMatch(node);
                } else if (spinCount > 0) {
                    spinCount--;//3: decrement spin count until 0
                } else if (timed) {//4:time parking
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime > spinForTimeoutThreshold) {
                        LockSupport.parkNanos(this, parkTime);
                    } else {
                        isFailed = true;//timeout
                    }
                } else {//5: parking without time
                    LockSupport.park(this);
                }

                //6: interruption test(not clear interrupted flag here)
                isFailed = currentThread.isInterrupted();
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
        private final int nodeType;
        private Node<E> prev;//unlink from this
        private volatile Node<E> next;
        private volatile Node match;
        private Thread waiter;

        Node(E item) {
            this.item = item;
            this.nodeType = item == null ? REQUEST : DATA;
        }

        private boolean isMatched() {
            return match != null;
        }

        private boolean casMatch(Node val) {
            return U.compareAndSwapObject(this, matchedOffset, null, val);
        }

        private boolean casNext(Node cmp, Node val) {
            return next == cmp && U.compareAndSwapObject(this, nextOffset, cmp, val);
        }
    }
}
