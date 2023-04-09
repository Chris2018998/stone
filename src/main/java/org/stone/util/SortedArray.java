/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sorted Array
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class SortedArray<E> {
    private final ReentrantLock arrayLock;
    private final Comparator<? super E> comparator;
    private int count;
    private E[] objects;

    public SortedArray(Class<E> type, int initSize, Comparator<E> comparator) {
        if (initSize < 0) throw new IllegalArgumentException("initSize < 0");
        if (type == null) throw new IllegalArgumentException("class type can't be null");
        if (comparator == null) throw new IllegalArgumentException("comparator can't be null");

        this.comparator = comparator;
        this.arrayLock = new ReentrantLock();
        this.objects = (E[]) Array.newInstance(type, initSize);
    }

    public static void main(String[] ags) {
        Comparator<Integer> comparator = new Comparator<Integer>() {
            public int compare(Integer e1, Integer e2) {
                return e1.compareTo(e2);
            }
        };
        SortedArray array = new SortedArray<Integer>(Integer.class, 5, comparator);
        array.add(5);
        array.print();
        array.remove(5);
        array.print();
//        array.add(2);
//        array.print();
//        array.add(3);
//        array.print();
//        array.add(1);
//        array.print();
//        array.add(7);
//        array.print();
//        array.remove(7);
//        array.print();
    }

    public int size() {
        arrayLock.lock();//lock array
        try {
            return count;
        } finally {
            arrayLock.unlock();//unlock
        }
    }

    public int add(E e) {
        arrayLock.lock();//lock array
        try {
            //1:if full,then grown
            if (objects.length == count) this.growArray();

            int pos = -1;
            if (count > 0) {
                for (int i = count - 1; i >= 0; i--) {
                    if (comparator.compare(e, objects[i]) >= 0) {
                        pos = i + 1;

                        System.arraycopy(this.objects, pos, objects, pos + 1, count - pos);
                        objects[pos] = e;
                        break;
                    }
                }

                //All elements move backward
                if (pos == -1) System.arraycopy(this.objects, 0, objects, 1, count);
            }

            if (pos == -1) objects[++pos] = e;

            count++;
            return pos;
        } finally {
            arrayLock.unlock();//unlock
        }
    }

    public int remove(E e) {
        arrayLock.lock();//lock array
        try {
            for (int i = count - 1; i >= 0; i--) {
                if (Objects.equals(e, objects[i])) {
                    System.arraycopy(this.objects, i + 1, objects, i, count - 1 - i);
                    count--;
                    return i;
                }
            }
            return -1;
        } finally {
            arrayLock.unlock();//unlock
        }
    }

    public void print() {
        arrayLock.lock();//lock array
        try {
            System.out.println("***** count:" + count);
            int lastPos = count - 1;
            for (int i = 0; i <= lastPos; i++) {
                System.out.println("array[" + i + "]:" + objects[i]);
            }
        } finally {
            arrayLock.unlock();//unlock
        }
    }

    private void growArray() {
        int oldCapacity = objects.length;
        int newCapacity = oldCapacity + (oldCapacity < 64 ?
                oldCapacity + 2 :
                oldCapacity >> 1);
        this.objects = Arrays.copyOf(objects, newCapacity);
    }

}
