/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.permit.impl.concurrent;

import org.jmin.util.UnsafeUtil;
import sun.misc.Unsafe;

import java.util.*;

/**
 * ConcurrentLinkedQueue2,A FIFO unbounded queue impl based on linked nodes,the queue has a fixed head node and remain a tail node(not physical remove)
 * <p>
 * 1: snapshot at queue creation(its shape like two sticks,so we call it Two-knot-Stick queue:双节棍队列)
 * <pre>
 *    +----------+                 +-----------+
 *   | head(null)| next --------> | tail(null)| next --------> null
 *   +-----------+                +-----------+
 * </pre>
 * <p>
 * 2:snapshot at queue offer one element(new tail node box contains element item)
 * <pre>
 *    +-----------+                +-------------+                  +---------------+
 *   | head(null) | next -------->|old tail(null)| next -------->  |new tail(item)| next --------> null
 *   +-----------+                +-------------+                  +---------------+
 * </pre>
 * <p>
 * 3:snapshot at queue poll tail node (just clear node item and set to null,then kept as empty box node)
 * <pre>
 *    +-----------+                  +---------------+
 *   | head(null) | next  -------->  |new tail(null) | next --------> null
 *   +-----------+                   +---------------+
 * </pre>
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ConcurrentLinkedQueue2<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {
    //***************************************************************************************************************//
    //                                           1: CAS Chain info                                                   //
    //***************************************************************************************************************//
    private final static Unsafe U;
    private final static long itemOffSet;
    private final static long nextOffSet;

    static {
        try {
            U = UnsafeUtil.getUnsafe();
            nextOffSet = U.objectFieldOffset(Node.class.getDeclaredField("next"));
            itemOffSet = U.objectFieldOffset(Node.class.getDeclaredField("item"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private transient final Node<E> head = new Node<E>(null);//fixed head
    private transient volatile Node<E> tail = new Node<E>(null);

    //***************************************************************************************************************//
    //                                          2: Constructors                                                      //
    //***************************************************************************************************************//
    public ConcurrentLinkedQueue2() {
        this.head.next = tail;
    }

    public ConcurrentLinkedQueue2(Collection<? extends E> c) {
        this.head.next = tail;
        if (c != null && c.size() > 0) {
            Node<E> prevNode = head;
            for (E e : c) {
                if (e == null) throw new NullPointerException();
                Node<E> newNode = new Node<E>(e);
                prevNode.next = newNode;
                this.tail = newNode;
                prevNode = newNode;
            }
        }
    }

    //***************************************************************************************************************//
    //                                          3: CAS Methods                                                       //
    //***************************************************************************************************************//
    //logic remove node
    private static boolean casItemToNull(Node node, Object cmp) {
        return U.compareAndSwapObject(node, itemOffSet, cmp, null);
    }

    private static boolean casTailNext(Node t, Node newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    //******************************************** link to next ******************************************************//
    //link next to target node
    private static void linkNextTo(Node startNode, Node endNode) {
        Node curNext = startNode.next;

        //startNode.next -----> endNode
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);
    }

    //physical remove skip node and link to its next node(if its next is null,then link to it)
    private static void linkNextToSkip(Node startNode, Node skipNode) {
        Node next = skipNode.next;
        Node endNode = next != null ? next : skipNode;

        //startNode.next -----> endNode
        Node curNext = startNode.next;
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);
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
        final Node<E> node = new Node<E>(e);
        Node<E> t;

        do {
            t = tail;//tail always exists
            if (t.next == null && casTailNext(t, node)) {//append to tail.next
                this.tail = node;//why? only place for tail change
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
        Node<E> prevNode = head;
        for (Node<E> curNode = prevNode.next; curNode != null; prevNode = curNode, curNode = curNode.next) {
            E item = curNode.item;
            if (item != null && casItemToNull(curNode, item)) {//failed means the node has removed by other thread
                linkNextToSkip(head, curNode);
                return item;
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
        Node<E> prevNode = head;
        for (Node<E> curNode = prevNode.next; curNode != null; prevNode = curNode, curNode = curNode.next) {
            E item = curNode.item;
            if (item != null) {
                linkNextTo(head, curNode);
                return item;
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
        for (Node node = head.next; node != null; node = node.next) {
            if (node.item != null) size++;
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
        for (Node<E> node = head.next; node != null; node = node.next) {
            E item = node.item;
            if (item != null && (item == o || o.equals(item))) return true;
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

        Node<E> segStartNode = null;//segment start node(reduce next cas)
        boolean find = false, removed = false;
        for (Node<E> prevNode = head, curNode = prevNode.next; curNode != null; prevNode = curNode, curNode = curNode.next) {
            E item = curNode.item;
            if (item == null) {
                if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
            } else {
                if (item == o || item.equals(o)) {
                    find = true;
                    //false,logic removed or polled by other thread,whether true or false,the current node has become invalid,so need physical remove from chain
                    removed = casItemToNull(curNode, item);//logic remove
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
        volatile Node<E> next;
        E iteratorItem;//next call in iterator,then copy item to this field

        Node(E e) {
            this.item = e;
        }
    }

    private class Itr implements Iterator<E> {
        private Node<E> curNode;
        private Node<E> startNode = head;

        public boolean hasNext() {
            curNode = searchNextNode(startNode);
            startNode = curNode;
            return curNode != null;
        }

        public E next() {
            if (curNode == null) throw new NoSuchElementException();
            if (curNode.item == null) throw new ConcurrentModificationException();
            return curNode.iteratorItem;
        }

        public void remove() {
            if (curNode == null) throw new NoSuchElementException();
            casItemToNull(curNode, curNode.iteratorItem);
        }

        private Node<E> searchNextNode(Node<E> startNode) {
            if (startNode == null) return null;

            Node<E> segStartNode = null;//segment start node
            for (Node<E> prevNode = startNode, curNode = prevNode.next; curNode != null; prevNode = curNode, curNode = curNode.next) {
                E item = curNode.item;
                if (item == null) {
                    if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
                } else {//end a segment
                    curNode.iteratorItem = item;
                    if (segStartNode != null) linkNextTo(segStartNode, curNode);//link next to current node

                    return curNode;
                }
            }//loop

            return null;
        }
    }
}