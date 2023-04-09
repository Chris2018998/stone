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
 * Sort Array
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class SortArray<E> {
    private final Comparator<? super E> comparator;
    private final ReentrantLock arrayLock;
    private int count;
    private E[] objects;

    public SortArray(Class<E> type, int initSize, Comparator<E> comparator) {
        this.comparator = comparator;
        this.arrayLock = new ReentrantLock();
        this.objects = (E[]) Array.newInstance(type, initSize);
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
            if (count == 0) {
                objects[0] = e;
                pos=0;
            } else {
                int lastPos = count - 1;
                for (int i = lastPos; i >= 0; i--) {
                    if (comparator.compare(e, objects[i]) >= 0) {
                        pos = i + 1;
                        break;
                    }
                }

                if (pos >= 0) {
                    System.arraycopy(this.objects, pos, objects, pos + 1, lastPos - pos + 1);
                    objects[pos] = e;
                } else {
                    System.arraycopy(this.objects, 0, objects, 1, count);
                    objects[0] = e;
                    pos=0;
                }
            }
            count++;
            return pos;
        } finally {
            arrayLock.unlock();//unlock
        }
    }

    public int remove(E e) {
        arrayLock.lock();//lock array
        try {
            int lastPos = count - 1;
            for (int i = lastPos; i >= 0; i--) {
                if (Objects.equals(e, objects[i])) {
                    System.arraycopy(this.objects, i + 1, objects, i, lastPos - i);
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
        objects = Arrays.copyOf(objects, newCapacity);
    }
}
