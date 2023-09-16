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

import org.stone.tools.unsafe.UnsafeAdaptor;
import org.stone.tools.unsafe.UnsafeAdaptorHolder;

import java.util.*;

/**
 * ConcurrentLinkedDeque Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConcurrentLinkedDeque<E> extends AbstractCollection<E> implements Deque<E>, java.io.Serializable {
    //***************************************************************************************************************//
    //                                           1: CAS Chain info                                                   //
    //***************************************************************************************************************//
    private final static UnsafeAdaptor U;
    private final static long itemOffSet;
    private final static long prevOffSet;
    private final static long nextOffSet;

    static {
        try {
            U = UnsafeAdaptorHolder.U;
            prevOffSet = U.objectFieldOffset(Node.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(Node.class.getDeclaredField("next"));
            itemOffSet = U.objectFieldOffset(Node.class.getDeclaredField("item"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private transient volatile Node<E> head = new Node<E>(null);
    private transient volatile Node<E> tail = new Node<E>(null);

    //***************************************************************************************************************//
    //                                          2: Constructors                                                      //
    //***************************************************************************************************************//

    public ConcurrentLinkedDeque() {
        head.next = tail;
        tail.prev = head;
    }

    public ConcurrentLinkedDeque(Collection<? extends E> c) {
        head.next = tail;
        tail.prev = head;

        if (c != null && c.size() > 0) {
            Node<E> preNode = head;
            for (E e : c) {
                if (e == null) throw new NullPointerException();
                Node<E> newNode = new Node<E>(e);
                preNode.next = newNode;
                newNode.prev = preNode;

                this.tail = newNode;
                preNode = newNode;
            }
        }
    }

    //***************************************************************************************************************//
    //                                          3: CAS Methods                                                       //
    //***************************************************************************************************************//
    private static boolean casItemToNull(Node node, Object cmp) {
        return U.compareAndSwapObject(node, itemOffSet, cmp, null);
    }

    private static boolean casTailNext(Node t, Node newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    private static boolean casHeadPrev(Node h, Node newPrev) {
        return U.compareAndSwapObject(h, prevOffSet, null, newPrev);
    }

    //******************************************** link to next ******************************************************//
    //link next to target node
    private static void linkNextTo(Node startNode, Node endNode) {
        //startNode.next ----> endNode
        Node curNext = startNode.next;
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        Node curPrev = endNode.prev;
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //physical remove skip node and link to its next node(if its next is null,then link to it)
    private static void linkNextToSkip(Node startNode, Node skipNode) {
        Node next = skipNode.next;
        Node endNode = next != null ? next : skipNode;

        //startNode.next -----> endNode
        Node curNext = startNode.next;
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev  -----> startNode
        Node curPrev = endNode.prev;
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //******************************************** link to prev ******************************************************//
    //link prev to target node
    private static void linkPrevTo(Node startNode, Node endNode) {
        //startNode.prev ----> endNode
        Node curPrev = startNode.prev;
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        Node curNext = endNode.next;
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //physical remove skip node and link to its prev node(if its prev is null,then link to it)
    private static void linkPrevToSkip(Node startNode, Node skipNode) {
        Node prev = skipNode.prev;
        Node endNode = prev != null ? prev : skipNode;

        //startNode.prev ----> endNode
        Node curPrev = startNode.prev;
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        Node curNext = endNode.next;
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //***************************************** get chain fist node and last node ************************************//
    private Node<E> getFirstNode() {
        Node<E> firstNode = head;//assume head is the first node

        do {
            Node<E> prevNode = firstNode.prev;
            if (prevNode == null) break;
            firstNode = prevNode;
        } while (true);

        return firstNode;
    }

    private Node<E> getLastNode() {
        Node<E> lastNode = tail;//assume tail is the last node

        do {
            Node<E> nextNode = lastNode.next;
            if (nextNode == null) break;
            lastNode = nextNode;
        } while (true);

        return lastNode;
    }

    //***************************************************************************************************************//
    //                                          4: Queue Methods                                                     //
    //***************************************************************************************************************//

    /**
     * Inserts the specified element at the front of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method {@link #offerFirst}.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null and this deque does not synchronizer null elements
     */
    public void addFirst(E e) {
        offerFirst(e);
    }

    /**
     * Inserts the specified element at the end of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method {@link #offerLast}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null and this deque does not synchronizer null elements
     */
    public void addLast(E e) {
        offerLast(e);
    }


    /**
     * Inserts the specified element at the front of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the {@link #addFirst} method,
     * which can fail to insert an element only by throwing an exception.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null and this deque does not synchronizer null elements
     */
    public boolean offerFirst(E e) {
        if (e == null) throw new NullPointerException();
        final Node<E> node = new Node<E>(e);
        Node<E> h;

        do {
            h = head;//head always exists
            if (h.prev == null && casHeadPrev(h, node)) {//append to head.pre
                node.next = h;
                this.head = node;//new head
                return true;
            }
        } while (true);
    }

    /**
     * Inserts the specified element at the end of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the {@link #addLast} method,
     * which can fail to insert an element only by throwing an exception.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null and this
     *                              deque does not synchronizer null elements
     */
    public boolean offerLast(E e) {
        if (e == null) throw new NullPointerException();
        final Node<E> node = new Node<E>(e);
        Node<E> t;

        do {
            t = tail;//tail always exists
            if (t.next == null && casTailNext(t, node)) {//append to tail.next
                node.prev = t;
                this.tail = node;//new tail
                return true;
            }
        } while (true);
    }

    /**
     * Retrieves and removes the first element of this deque.  This method
     * differs from {@link #pollFirst pollFirst} only in that it throws an
     * exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    public E removeFirst() {
        E e = pollFirst();
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    /**
     * Retrieves and removes the last element of this deque.  This method
     * differs from {@link #pollLast pollLast} only in that it throws an
     * exception if this deque is empty.
     *
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    public E removeLast() {
        E e = pollLast();
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    /**
     * Retrieves and removes the first element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    public E pollFirst() {
        Node<E> prevNode = null;
        final Node<E> firstNode = this.getFirstNode();
        for (Node<E> curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.next) {
            E item = curNode.item;
            if (item != null && casItemToNull(curNode, item)) {//failed means the node has removed by other thread
                linkNextToSkip(firstNode, curNode);
                return item;
            }
        }//loop for

        if (prevNode != null) linkNextToSkip(firstNode, prevNode);
        return null;
    }

    /**
     * Retrieves and removes the last element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the tail of this deque, or {@code null} if this deque is empty
     */
    public E pollLast() {
        Node<E> nextNode = null;
        final Node<E> lastNode = this.getLastNode();

        for (Node<E> curNode = lastNode; curNode != null; nextNode = curNode, curNode = curNode.prev) {
            E item = curNode.item;
            if (item != null && casItemToNull(curNode, item)) {//failed means the node has removed by other thread
                linkPrevToSkip(lastNode, curNode);
                return item;
            }
        }//loop for

        if (nextNode != null) linkPrevToSkip(lastNode, nextNode);
        return null;
    }

    /**
     * Retrieves, but does not remove, the first element of this deque.
     * <p>
     * This method differs from {@link #peekFirst peekFirst} only in that it
     * throws an exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    public E getFirst() {
        E e = peekFirst();
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    /**
     * Retrieves, but does not remove, the last element of this deque.
     * This method differs from {@link #peekLast peekLast} only in that it
     * throws an exception if this deque is empty.
     *
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    public E getLast() {
        E e = peekLast();
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    /**
     * Retrieves, but does not remove, the first element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    public E peekFirst() {
        Node<E> prevNode = null;
        final Node<E> firstNode = this.getFirstNode();
        for (Node<E> curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.next) {
            E item = curNode.item;
            if (item != null) {//failed means the node has removed by other thread
                if (firstNode != curNode) linkNextTo(firstNode, curNode);
                return item;
            }
        }//loop for

        //prevNode: from head to tail(asc)
        if (prevNode != null) linkNextToSkip(firstNode, prevNode);
        return null;
    }

    /**
     * Retrieves, but does not remove, the last element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the tail of this deque, or {@code null} if this deque is empty
     */
    public E peekLast() {
        Node<E> nextNode = null;
        final Node<E> lastNode = this.getLastNode();

        for (Node<E> curNode = lastNode; curNode != null; nextNode = curNode, curNode = curNode.prev) {
            E item = curNode.item;
            if (item != null) {//failed means the node has removed by other thread
                if (curNode != lastNode) linkPrevTo(lastNode, curNode);
                return item;
            }
        }//loop for

        //nextNode: from tail to head(desc)
        if (nextNode != null) linkPrevToSkip(lastNode, nextNode);
        return null;
    }

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException   if the class of the specified element
     *                              is incompatible with this deque
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              deque does not synchronizer null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) throw new NullPointerException();

        Node<E> prevNode = null;
        Node<E> segStartNode = null;//segment start node
        boolean find = false, removed = false;
        final Node<E> firstNode = this.getFirstNode();

        //loop Iteration direction: head ---> tail
        for (Node<E> curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.next) {
            E item = curNode.item;
            if (item == null) {
                if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
            } else {
                if (item == o || item.equals(o)) {
                    find = true;
                    //false,logic removed or polled by other thread,whether true or false,the current node has become invalid,so need physical remove from chain
                    removed = casItemToNull(curNode, item);//logic remove
                }

                if (segStartNode != null) {//end a segment
                    if (find) {
                        linkNextToSkip(segStartNode, curNode);//link to current node 'next
                        return removed;
                    } else
                        linkNextTo(segStartNode, curNode);//link to current node
                    segStartNode = null;
                } else if (find) {//preNode is a valid node
                    if (prevNode != null) linkNextToSkip(prevNode, curNode);
                    return removed;
                }
            }
        }//loop

        if (segStartNode != null) linkNextToSkip(segStartNode, prevNode);

        return false;
    }

    /**
     * Removes the last occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException   if the class of the specified element
     *                              is incompatible with this deque
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              deque does not synchronizer null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    public boolean removeLastOccurrence(Object o) {
        if (o == null) throw new NullPointerException();

        Node<E> nextNode = null;
        Node<E> segStartNode = null;//segment start node
        boolean find = false, removed = false;
        final Node<E> lastNode = this.getLastNode();

        //loop Iteration direction: tail ---> head
        for (Node<E> curNode = lastNode; curNode != null; nextNode = curNode, curNode = curNode.prev) {
            E item = curNode.item;
            if (item == null) {
                if (segStartNode == null) segStartNode = nextNode;//mark as start node of a segment
            } else {
                if (item == o || item.equals(o)) {
                    find = true;
                    //false,logic removed or polled by other thread,whether true or false,the current node has become invalid,so need physical remove from chain
                    removed = casItemToNull(curNode, item);//logic remove
                }

                if (segStartNode != null) {//end a segment
                    if (find) {
                        linkPrevToSkip(segStartNode, curNode);
                        return removed;
                    } else
                        linkPrevTo(segStartNode, curNode);

                    segStartNode = null;
                } else if (find) {//preNode is a valid node
                    if (nextNode != null) linkPrevToSkip(nextNode, curNode);
                    return removed;
                }
            }
        }//loop

        if (segStartNode != null) linkPrevToSkip(segStartNode, nextNode);

        return false;
    }

    // *** Queue methods ***

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an
     * {@code IllegalStateException} if no space is currently available.
     * When using a capacity-restricted deque, it is generally preferable to
     * use {@link #offer(Object) offer}.
     *
     * <p>This method is equivalent to {@link #addLast}.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException    if the element cannot be added at this
     *                                  parkTime due to capacity restrictions
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this deque
     * @throws NullPointerException     if the specified element is null and this
     *                                  deque does not synchronizer null elements
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this deque
     */
    public boolean add(E e) {
        return this.offer(e);
    }

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.  When using a capacity-restricted deque, this method is
     * generally preferable to the {@link #add} method, which can fail to
     * insert an element only by throwing an exception.
     *
     * <p>This method is equivalent to {@link #offerLast}.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     * {@code false}
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this deque
     * @throws NullPointerException     if the specified element is null and this
     *                                  deque does not synchronizer null elements
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this deque
     */
    public boolean offer(E e) {
        return this.offerLast(e);
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque).
     * This method differs from {@link #poll poll} only in that it throws an
     * exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    public E remove() {
        E e = removeFirst();
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), or returns
     * {@code null} if this deque is empty.
     *
     * <p>This method is equivalent to {@link #pollFirst()}.
     *
     * @return the first element of this deque, or {@code null} if
     * this deque is empty
     */
    public E poll() {
        return pollFirst();
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque).
     * This method differs from {@link #peek peek} only in that it throws an
     * exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #getFirst()}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    public E element() {
        return this.getFirst();
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque), or
     * returns {@code null} if this deque is empty.
     *
     * <p>This method is equivalent to {@link #peekFirst()}.
     *
     * @return the head of the queue represented by this deque, or
     * {@code null} if this deque is empty
     */
    public E peek() {
        return this.peekFirst();
    }

    // *** Stack methods ***

    /**
     * Pushes an element onto the stack represented by this deque (in other
     * words, at the head of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, throwing an
     * {@code IllegalStateException} if no space is currently available.
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @throws IllegalStateException    if the element cannot be added at this
     *                                  parkTime due to capacity restrictions
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this deque
     * @throws NullPointerException     if the specified element is null and this
     *                                  deque does not synchronizer null elements
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this deque
     */
    public void push(E e) {
        this.addFirst(e);
    }

    /**
     * Pops an element from the stack represented by this deque.  In other
     * words, removes and returns the first element of this deque.
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this deque (which is the top
     * of the stack represented by this deque)
     * @throws NoSuchElementException if this deque is empty
     */
    public E pop() {
        E e = this.removeFirst();
        if (e == null) throw new NoSuchElementException();
        return e;
    }


    //***************************************************************************************************************//
    //                                          5: Collection Methods                                                //
    //***************************************************************************************************************//

    // *** Collection methods ***

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * <p>This method is equivalent to {@link #removeFirstOccurrence(Object)}.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException   if the class of the specified element
     *                              is incompatible with this deque
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              deque does not synchronizer null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }


    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        List elementList = new LinkedList();
        for (Node node = head.next; node != null; node = node.next) {
            Object item = node.item;
            if (item != null) elementList.add(item);
        }
        return elementList.toArray();
    }

    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the queue fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this queue.
     *
     * <p>If this queue fits in the specified array with room to spare
     * (i.e., the array has more elements than this queue), the element in
     * the array immediately following the end of the queue is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     * <p>
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this queue
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        if (a == null) throw new NullPointerException();
        int i = 0;
        int arraySize = a.length;
        for (Node node = head.next; node != null; node = node.next) {
            Object item = node.item;
            if (item != null) a[i++] = (T) node.item;
            if (i == arraySize) break;
        }
        return a;
    }

    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this deque is to be tested
     * @return {@code true} if this deque contains the specified element
     * @throws ClassCastException   if the type of the specified element
     *                              is incompatible with this deque
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              deque does not synchronizer null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node<E> node = head.next; node != null; node = node.next) {
            E item = node.item;
            if (item != null && (item == o || o.equals(item))) return true;
        }
        return false;
    }

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    public int size() {
        int size = 0;
        for (Node node = head.next; node != null; node = node.next) {
            if (node.item != null) size++;
            if (size == Integer.MAX_VALUE) break;
        }
        return size;
    }

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    public Iterator<E> iterator() {
        return new AscItr();
    }

    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *
     * @return an iterator over the elements in this deque in reverse
     * sequence
     */
    public Iterator<E> descendingIterator() {
        return new DescItr();
    }

    //***************************************************************************************************************//
    //                                          6: Inner class                                                       //
    //***************************************************************************************************************//
    private static class Node<E> {
        volatile E item;
        volatile Node<E> prev;
        volatile Node<E> next;
        E ascIteratorItem;
        E descIteratorItem;

        Node(E e) {
            this.item = e;
        }
    }

    private class AscItr implements Iterator<E> {
        private Node<E> curNode;
        private Node<E> startNode;
        private boolean firstNext = true;

        AscItr() {
            startNode = getFirstNode();
        }

        public boolean hasNext() {
            curNode = searchNextNode(startNode, firstNext);
            if (firstNext) firstNext = false;

            startNode = curNode;
            return curNode != null;
        }

        public E next() {
            if (curNode == null) throw new NoSuchElementException();
            if (curNode.item == null) throw new ConcurrentModificationException();
            return curNode.ascIteratorItem;
        }

        public void remove() {
            if (curNode == null) throw new NoSuchElementException();
            casItemToNull(curNode, curNode.ascIteratorItem);
        }

        private Node<E> searchNextNode(Node<E> startNode, boolean isFirst) {
            if (startNode == null) return null;

            Node<E> prevNode = startNode;
            Node<E> segStartNode = null;//segment start node

            //loop Iteration direction: startNode ---> tail
            for (Node<E> curNode = isFirst ? startNode : startNode.next; curNode != null; prevNode = curNode, curNode = curNode.next) {
                E item = curNode.item;
                if (item == null) {
                    if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
                } else {//find valid node
                    curNode.ascIteratorItem = item;

                    if (segStartNode != null) linkNextTo(segStartNode, curNode);//link next to current node

                    return curNode;
                }
            }//loop

            return null;
        }
    }

    private class DescItr implements Iterator<E> {
        private Node<E> startNode;
        private Node<E> curNode;
        private boolean firstNext = true;

        DescItr() {
            this.startNode = getLastNode();
        }

        public boolean hasNext() {
            curNode = searchPrevNode(startNode, firstNext);
            if (firstNext) firstNext = false;

            startNode = curNode;//next start node
            return curNode != null;
        }

        public E next() {
            if (curNode == null) throw new NoSuchElementException();
            if (curNode.item == null) throw new ConcurrentModificationException();
            return curNode.descIteratorItem;
        }

        public void remove() {
            if (curNode == null) throw new NoSuchElementException();
            casItemToNull(curNode, curNode.descIteratorItem);
        }

        private Node<E> searchPrevNode(Node<E> startNode, boolean isFirst) {
            if (startNode == null) return null;

            Node<E> nextNode = startNode;
            Node<E> segStartNode = null;//segment start node

            //loop Iteration direction: startNode ---> head
            for (Node<E> curNode = isFirst ? startNode : startNode.prev; curNode != null; nextNode = curNode, curNode = curNode.prev) {
                E item = curNode.item;
                if (item == null) {
                    if (segStartNode == null) segStartNode = nextNode;//mark as start node of a segment
                } else {//find valid node
                    curNode.descIteratorItem = item;

                    if (segStartNode != null) linkPrevTo(segStartNode, curNode);//link prev to current node

                    return curNode;
                }
            }//loop

            return null;
        }
    }
}
