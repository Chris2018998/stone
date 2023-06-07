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
        return matcher.tryMatch(new Node<>(null));
    }

    public E take() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();

        E matchedItem = matcher.match(new Node<>(null), 0);

        boolean isInterrupted = Thread.interrupted();//clear interrupted status
        if (matchedItem == null && isInterrupted) throw new InterruptedException();
        return matchedItem;
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();

        E matchedItem = matcher.match(new Node<>(null), unit.toNanos(timeout));

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
    private interface BufferMatcher<E> {

        E tryMatch(Node<E> e);

        E match(Node<E> e, long timeoutNanos);
    }

    //****************************************************************************************************************//
    //                                      6: Matcher Impl By Stack                                                  //
    //****************************************************************************************************************//
    private static final class StackMatcher<E> implements BufferMatcher<E> {
        private static final long headOffset;

        static {
            try {
                Class<?> k = StackMatcher.class;
                headOffset = U.objectFieldOffset
                        (k.getDeclaredField("head"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        private transient volatile Node<E> head = new Node<>(null);

        //******************************* 6.1: Chain Cas(1)***********************************************************//
        private void casHead(Node oldHead, Node newHead) {
            if (oldHead == head) U.compareAndSwapObject(this, headOffset, oldHead, newHead);
        }

        //******************************* 6.2: tryMatch **************************************************************//
        public E tryMatch(Node<E> node) {
            int nodeTye = node.nodeType;

            do {
                //1: read head
                Node<E> h = head;

                //2: exit loop when meet same type node
                if (h == null || h.nodeType == nodeTye) return null;

                //3: try to match current node
                if (h.match == null && h.casMatch(node)) {//match success
                    Node<E> next = h.next;
                    if (next != null) casHead(h, next);
                    return h.nodeType == DATA ? h.item : node.item;
                }
            } while (true);
        }

        //******************************* 6.3: match *****************************************************************//
        public E match(Node<E> node, long timeoutNanos) {
//            E matchedItem;
//            int nodeTye = node.nodeType;
//
//            do {
//                Node<E> h = head;
//                if (h == null || h.nodeType == nodeTye) {//empty or same type
//                    //1: offer to chain
//                    this.offerToChain(node);
//                    //2: wait for matching
//                    Node<E> matched = this.waitForFilling(node, timeoutNanos);
//                    //3: matched success
//                    if (matched != node) return matched.nodeType == DATA ? matched.item : node.item;
//
//                } else if ((matchedItem = tryMatch(node)) != null) {//match transfer
//                    return matchedItem;
//                }
//            } while (true);

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

        private transient volatile Node<E> head;
        private transient volatile Node<E> tail;

        //******************************* 7.1: Chain Cas(2)***********************************************************//
        private boolean casHead(Node oldHead, Node newHead) {
            return oldHead == head && U.compareAndSwapObject(this, headOffset, oldHead, newHead);
        }

        private void casTail(Node oldTail, Node newTail) {
            if (oldTail == tail) U.compareAndSwapObject(this, tailOffset, oldTail, newTail);
        }

        //******************************* 7.2: tryMatch **************************************************************//
        public final E tryMatch(Node<E> node) {
            Node<E> curNode = head;
            int nodeTye = node.nodeType;

            do {
                //1: exit loop when meet same type node
                if (curNode == null || curNode.nodeType == nodeTye) return null;

                //2: try to match current node
                if (curNode.match == null && curNode.casMatch(node)) {//match success
                    Node<E> h = head;
                    Node<E> next = curNode.next;
                    if (next != null)
                        casHead(h, next);
                    else if (h != curNode)
                        casHead(h, curNode);
                    return curNode.nodeType == DATA ? curNode.item : node.item;
                }

                //3: read next node
                curNode = curNode.next;
            } while (true);
        }

        //******************************* 7.3: match *****************************************************************//
        public final E match(Node<E> node, long timeout) {
            E matchedItem;
            int type = node.nodeType;

            do {
                Node<E> t = tail;
                if (t == head || t.isMatched() || t.nodeType == type) {//empty or same type
                    //1: offer to chain
                    this.offerToChain(node);
                    //2: wait for matching
                    Node<E> matched = this.waitForFilling(node, timeout);
                    //3: matched success
                    if (matched != node) return matched.nodeType == DATA ? matched.item : node.item;

                    //4: remove cancelled node from chain
                    Node prev = node.prev;
                    Node next = node.next;
                    if (prev != null) {
                        if (next != null) prev.casNext(node, next);
                    } else if (next != null) {//node is head
                        casHead(head, next);
                    }
                    return null;
                } else if ((matchedItem = tryMatch(node)) != null) {//match transfer
                    return matchedItem;
                }
            } while (true);
        }

        //******************************* 7.4: offer to chain ********************************************************//
        private void offerToChain(Node<E> node) {
            if (node.waiter == null) node.waiter = Thread.currentThread();

            do {
                Node<E> t = tail;
                if (t != null) {
                    node.prev = t;
                    if ((t.nodeType == node.nodeType || t.match != null) && t.casNext(null, node)) {
                        casTail(t, node);
                        return;
                    }
                } else if (head == null && casHead(null, node)) {
                    this.tail = node;
                    return;
                }
            } while (true);
        }

        //******************************* 7.5: Wait for being matched ************************************************//
        private Node<E> waitForFilling(Node<E> node, long timeout) {
            boolean isFailed = false;//interrupted or timeout,cancel node by self
            Thread currentThread = node.waiter;
            boolean timed = timeout > 0;
            long deadline = timed ? System.nanoTime() + timeout : 0;
            int spinCount = head.next == node ? (timed ? maxTimedSpins : maxUntimedSpins) : 0;//spin on head node

            do {
                //1: read match node
                Node<E> matched = node.match;
                if (matched != null) return matched;

                //2: cancel node when failed
                if (isFailed) {
                    node.casMatch(node);
                } else if (spinCount > 0) {
                    spinCount--;//3: decrement spin count until 0
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
        private final int nodeType;
        private Node<E> prev;//unlink from this
        private volatile Node<E> next;
        private volatile Node<E> match;
        private Thread waiter;

        Node(E item) {
            this.item = item;
            this.nodeType = item == null ? REQUEST : DATA;
        }

        private boolean isMatched() {
            return match != null && match != this;
        }

        private boolean casMatch(Node val) {
            return U.compareAndSwapObject(this, matchedOffset, null, val);
        }

        private boolean casNext(Node cmp, Node val) {
            return next == cmp && U.compareAndSwapObject(this, nextOffset, cmp, val);
        }
    }
}
