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

import static org.stone.shine.synchronizer.CasNodeUpdater.casNext;
import static org.stone.shine.synchronizer.CasNodeUpdater.casState;
import static org.stone.shine.synchronizer.CasStaticState.REMOVED;

/**
 * ConcurrentLinkedQueue
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {
    private transient final CasNode head = new CasNode(null);//fixed head
    private transient volatile CasNode tail = head;

    //****************************************************************************************************************//
    //                                          1: Constructors                                                       //
    //****************************************************************************************************************//
    public ConcurrentLinkedQueue() {
    }

    public ConcurrentLinkedQueue(Collection<? extends E> c) {
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

    //****************************************************************************************************************//
    //                                          2: Queue Methods                                                      //
    //****************************************************************************************************************//

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
        CasNode curNode = head.getNext();
        if (curNode == null) return null;

        do {
            //1: node cas check
            Object item = curNode.getState();
            if (item != REMOVED && casState(curNode, item, REMOVED)) {//logic removed success
                CasNode headOldNext = head.getNext();
                CasNode headNewNext = curNode.getNext();
                if (casNext(head, headOldNext, headNewNext) && headNewNext == null)
                    this.tail = head;
                return (E) item;
            }

            //2: chain last node check
            CasNode nextNode = curNode.getNext();
            if (nextNode == null) {//reach last node
                CasNode headOldNext = head.getNext();
                if (casNext(head, headOldNext, null))
                    this.tail = head;
                return null;
            }
            curNode = nextNode;
        } while (true);
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public E peek() {
        CasNode curNode = head.getNext();
        if (curNode == null) return null;

        do {
            //1: node cas check
            Object item = curNode.getState();
            if (item != REMOVED) {//logic removed success
                CasNode headOldNext = head.getNext();
                //means that nodes before current node have been logic removed,so need remove them from chain
                if (headOldNext != curNode) casNext(head, headOldNext, curNode);
                return (E) item;
            }

            //2: chain last node check
            CasNode nextNode = curNode.getNext();
            if (nextNode == null) {//reach last node
                CasNode headOldNext = head.getNext();
                if (casNext(head, headOldNext, null))
                    this.tail = head;
                return null;
            }
            curNode = nextNode;
        } while (true);
    }

    //****************************************************************************************************************//
    //                                         3: Collection Methods                                                  //
    //****************************************************************************************************************//

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
        CasNode curNode = head.getNext();
        if (curNode == null) return false;

        CasNode prevNode = head;
        CasNode segStartNode = null;
        do {
            Object item = curNode.getState();
            if (item == REMOVED) {//current node has been logic removed
                if (segStartNode == null) segStartNode = prevNode;
            } else if (item == o || item.equals(o)) {//need remove the node from chain
                boolean removed = casState(curNode, item, REMOVED);//logic remove
                if (segStartNode == null) segStartNode = prevNode;
                CasNode segOldNext = segStartNode.getNext();
                CasNode segNewNext = curNode.getNext();
                if (casNext(segStartNode, segOldNext, segNewNext) && segNewNext == null)
                    this.tail = segStartNode;
                return removed;
            } else if (segStartNode != null) {//link segStartNode next to the current node
                CasNode segOldNext = segStartNode.getNext();
                casNext(segStartNode, segOldNext, curNode);
                segStartNode = null;
            }

            //update current node for next loop
            prevNode = curNode;
            curNode = curNode.getNext();
            if (curNode == null) {//has reach the end of chain
                if (segStartNode == null) segStartNode = prevNode;
                CasNode segOldNext = segStartNode.getNext();
                if (casNext(segStartNode, segOldNext, null))
                    this.tail = segStartNode;
                return false;
            }
        } while (true);
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
        return new Itr<>(this);
    }

    //****************************************************************************************************************//
    //                                          4: Iterator impl                                                      //
    //****************************************************************************************************************//
    private static class Itr<E> implements Iterator<E> {
        private final ChainPointer<E> chainPointer;

        Itr(ConcurrentLinkedQueue queue) {
            this.chainPointer = new ChainPointer<>(queue);
        }

        //search a valid node after current node and fill result to the pointer
        private static <E> void searchNextNode(ChainPointer pointer) {
            CasNode searchedNode = null;
            E searchedNodeItem = null;

            try {
                CasNode startNode = pointer.node;
                if (startNode == null) return;
                //if current node is null,set start node
                CasNode curNode = startNode.getNext();
                if (curNode == null) return;

                do {
                    Object item = curNode.getState();
                    if (item != REMOVED) {//find a valid node
                        CasNode startOldNext = startNode.getNext();
                        //means that nodes before current node have been logic removed,so need remove them from chain
                        if (startOldNext != curNode) casNext(startNode, startOldNext, curNode);
                        searchedNode = curNode;
                        searchedNodeItem = (E) item;
                        break;
                    }

                    //2: chain last node check
                    CasNode nextNode = curNode.getNext();
                    if (nextNode == null) {//reach last node
                        CasNode startOldNext = startNode.getNext();
                        if (casNext(startNode, startOldNext, null))
                            pointer.queue.tail = startNode;
                        break;
                    }
                    curNode = nextNode;
                } while (true);
            } finally {
                pointer.fill(searchedNode, searchedNodeItem);
            }
        }

        //remove current point node from chain
        public void remove() {
            CasNode curNode = chainPointer.node;
            if (curNode == null) throw new NoSuchElementException();
            casState(curNode, chainPointer.nodeItem, REMOVED);
        }

        //check exists a valid node(not logic removed) after current node
        public boolean hasNext() {
            searchNextNode(chainPointer);
            return chainPointer.node != null;
        }

        //return current node item
        public E next() {
            //1:check current node and item
            CasNode curNode = chainPointer.node;
            if (curNode == null) throw new NoSuchElementException();
            Object item = curNode.getState();
            if (item != REMOVED) return (E) item;//valid node

            //2:re-get a valid node from chain
            searchNextNode(chainPointer);
            if (chainPointer.node == null) throw new ConcurrentModificationException();
            return chainPointer.nodeItem;
        }
    }

    private static class ChainPointer<E> {
        private final ConcurrentLinkedQueue queue;
        private CasNode node;
        private E nodeItem;

        ChainPointer(ConcurrentLinkedQueue queue) {
            this.queue = queue;
            this.node = queue.head;
        }

        void fill(CasNode node, E nodeItem) {
            this.node = node;
            this.nodeItem = nodeItem;
        }
    }
}