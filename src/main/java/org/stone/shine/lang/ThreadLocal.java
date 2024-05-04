/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.stone.shine.lang;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *
 * @author Josh Bloch and Doug Lea
 * @since 1.2
 */
public class ThreadLocal<T> {
    private static final int HASH_INCREMENT = 0x61c88647;
    private static final AtomicInteger nextHashCode = new AtomicInteger();

    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    protected T initialValue() {
        return null;
    }

    public T get() {
        //modify by chris 2023-06-23 begin
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        return map != null ? (T) map.get(this) : initialValue();
        //***chris  2023-06-23 end

//        Thread t = Thread.currentThread();
//        ThreadLocalMap map = getMap(t);
//        if (map != null) {
//            ThreadLocalMap.Entry e = map.getEntry(this);
//            if (e != null) {
//                @SuppressWarnings("unchecked")
//                T result = (T)e.value;
//                return result;
//            }
//        }
//        return setInitialValue();
    }

    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    public void remove() {
        ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null) m.remove(this);
    }

    ThreadLocalMap getMap(Thread t) {
        //return t.threadLocals;
        return null;
    }

    void createMap(Thread t, T firstValue) {
        //t.threadLocals = new ThreadLocalMap(this, firstValue);
    }
}
