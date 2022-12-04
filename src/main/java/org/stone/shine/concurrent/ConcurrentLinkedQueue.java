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

import org.stone.shine.synchronizer.CasNode;

import java.util.*;

import static org.stone.shine.synchronizer.CasNodeUpdater.*;
import static org.stone.shine.synchronizer.CasStaticState.REMOVED;

/**
 * ConcurrentLinkedQueue,A FIFO unbounded queue impl based on linked nodes,the queue has a fixed head node and remain a tail node(not physical remove)
 * <p>
 * 1: snapshot at queue creation(its shape like two sticks,so we call it Two-knot-Stick queue:双节棍队列)
 * ({@code
 * +----------+                 +-----------+
 * | head(null)| next --------> | tail(null)| next --------> null
 * +-----------+                +-----------+
 * })
 * <p>
 * 2:snapshot at queue offer one element(new tail node box contains element item)
 * ({@code
 * +-----------+                +-------------+                  +---------------+
 * | head(null) | next -------->|old tail(null)| next -------->  |new tail(item)| next --------> null
 * +-----------+                +-------------+                  +---------------+
 * })
 * <p>
 * 3:snapshot at queue poll tail node (just clear node item and set to null,then kept as empty box node)
 * ({@code
 * +-----------+                  +---------------+
 * | head(null) | next  -------->  |new tail(null) | next --------> null
 * +-----------+                   +---------------+
 * })
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {
    private transient final CasNode head = new CasNode(null);//fixed head
    private transient volatile CasNode tail = new CasNode(null);

    //***************************************************************************************************************//
    //                                          1: Constructors                                                      //
    //***************************************************************************************************************//
    public ConcurrentLinkedQueue() {
        this.head.setNext(tail);
    }

    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        this.head.setNext(tail);
        if (c != null && c.size() > 0) {
            CasNode prevNode = head;
            for (E e : c) {
                if (e == null) throw new NullPointerException();
                CasNode newNode = new CasNode(e);
                prevNode.setNext(newNode);
                prevNode = newNode;
                this.tail = newNode;
            }
        }
    }

    //***************************************************************************************************************//
    //                                          4: Queue Methods                                                     //
    //***************************************************************************************************************//

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
        final CasNode node = new CasNode(e);
        CasNode t;

        do {
            t = tail;//tail always exists
            if (t.getNext() == null && casNext(t, null, node)) {//append to tail.next
                this.tail = node;
                return true;
            }
        } while (true);
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public E poll() {
        CasNode prevNode = head;
        for (CasNode curNode = prevNode.getNext(); curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            Object item = curNode.getState();
            if (item != REMOVED && logicRemove(curNode)) {//failed means the node has removed by other thread
                linkNextToSkip(head, curNode);
                return (E) item;
            }
        }//loop for

        //poll fail,then try to clean logic deleted nodes between head and prevNode(the last node in loop)
        linkNextToSkip(head, prevNode);
        return null;
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public E peek() {
        CasNode prevNode = head;
        for (CasNode curNode = prevNode.getNext(); curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            Object item = curNode.getState();
            if (item != REMOVED) {//failed means the node has removed by other thread
                linkNextToSkip(head, curNode);
                return (E) item;
            }
        }//loop for

        //peek fail,try link to last node in loop
        linkNextToSkip(head, prevNode);
        return null;
    }

    //***************************************************************************************************************//
    //                                          5: Collection Methods                                                //
    //***************************************************************************************************************//

    /**
     * @return {@code true} if this queue contains no elements
     */
    public boolean isEmpty() {
        return peek() == null;
    }

    /**
     * Returns valid node size of chain
     */
    public int size() {
        int size = 0;
        for (CasNode node = head.getNext(); node != null; node = node.getNext()) {
            if (node.getState() != REMOVED) size++;
            if (size == Integer.MAX_VALUE) break;
        }
        return size;
    }

    /**
     * <p>This implementation iterates over the elements in the collection,
     * checking each element in turn for equality with the specified element
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        for (CasNode node = head.getNext(); node != null; node = node.getNext()) {
            Object item = node.getState();
            if (item != REMOVED && (item == o || o.equals(item))) return true;
        }
        return false;
    }

    /**
     * <p>This implementation iterates over the collection looking for the
     * specified element.  If it finds the element, it removes the element
     * from the collection using the iterator's remove method.
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by this
     * collection's iterator method does not implement the <tt>remove</tt>
     * method and this collection contains the specified object.
     * <p>
     * if need remove element is null then throws NullPointerException
     */
    public boolean remove(Object o) {
        if (o == null) throw new NullPointerException();

        CasNode segStartNode = null;//segment start node(reduce next cas)
        boolean find = false, removed = false;
        for (CasNode prevNode = head, curNode = prevNode.getNext(); curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            Object item = curNode.getState();

            if (item == REMOVED) {
                if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
            } else {
                if (item == o || item.equals(o)) {
                    find = true;
                    //false,logic removed or polled by other thread,whether true or false,the current node has become invalid,so need physical remove from chain
                    removed = logicRemove(curNode);//logic remove
                }

                if (segStartNode != null) {//end of a segment
                    if (find) {
                        linkNextToSkip(segStartNode, curNode);//link to current node 'next
                        return removed;
                    } else
                        linkNextTo(segStartNode, curNode);//link to current node

                    segStartNode = null;
                } else if (find) {//prevNode is a valid node
                    linkNextToSkip(prevNode, curNode);
                    return removed;
                }
            }
        }//loop

        if (segStartNode != null) linkNextToSkip(segStartNode, tail);//link to tail

        return false;
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
        for (CasNode node = head.getNext(); node != null; node = node.getNext()) {
            Object item = node.getState();
            if (item != REMOVED) elementList.add(item);
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
        for (CasNode node = head.getNext(); node != null; node = node.getNext()) {
            Object item = node.getState();
            if (item != REMOVED) a[i++] = (T) item;
            if (i == arraySize) break;
        }
        return a;
    }

    /**
     * Returns an iterator over the elements contained in this collection.
     */
    public Iterator<E> iterator() {
        return new Itr();
    }


    //***************************************************************************************************************//
    //                                          6: Inner class                                                       //
    //***************************************************************************************************************//
    private static class Node<E> {
        volatile E item;//null means abandon and need remove from chain(exclude tail)
        volatile CasNode next;
        E iteratorItem;//next call in iterator,then copy item to this field

        Node(E e) {
            this.item = e;
        }
    }

    private class Itr implements Iterator<E> {
        private CasNode curNode;
        private CasNode startNode = head;

        public boolean hasNext() {
            curNode = getNextNode(startNode);
            startNode = curNode;
            return curNode != null;
        }

        public E next() {
            if (curNode == null) throw new NoSuchElementException();
            if (curNode.getState() == REMOVED) throw new ConcurrentModificationException();

            //@todo
            return null;
            //return curNode.iteratorItem;
        }

        public void remove() {
            if (curNode == null) throw new NoSuchElementException();
            logicRemove(curNode);
        }

        private CasNode getNextNode(CasNode startNode) {
            if (startNode == null) return null;

            CasNode segStartNode = null;//segment start node
            for (CasNode prevNode = startNode, curNode = prevNode.getNext(); curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
                Object item = curNode.getState();

                if (item == REMOVED) {
                    if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
                } else {//end a segment
                    // curNode.iteratorItem = item;
                    //@todo

                    //return curNode.iteratorItem;

                    if (segStartNode != null) linkNextTo(segStartNode, curNode);//link next to current node

                    return curNode;
                }
            }//loop

            return null;
        }
    }
}