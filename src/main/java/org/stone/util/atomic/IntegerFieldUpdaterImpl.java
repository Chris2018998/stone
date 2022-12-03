/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.util.atomic;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Atomic Integer Field Updater Implementation(Don't use in other place)
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class IntegerFieldUpdaterImpl<T> extends AtomicIntegerFieldUpdater<T> {
    private final static UnsafeAdaptor unsafe = UnsafeAdaptorFactory.get();
    private final long offset;

    private IntegerFieldUpdaterImpl(long offset) {
        this.offset = offset;
    }

    public static <T> AtomicIntegerFieldUpdater<T> newUpdater(Class<T> beanClass, String fieldName) {
        try {
            return new IntegerFieldUpdaterImpl<T>(unsafe.objectFieldOffset(beanClass.getDeclaredField(fieldName)));
        } catch (Throwable e) {
            return AtomicIntegerFieldUpdater.newUpdater(beanClass, fieldName);
        }
    }

    public final boolean compareAndSet(T bean, int expect, int update) {
        return unsafe.compareAndSwapInt(bean, this.offset, expect, update);
    }

    public final boolean weakCompareAndSet(T bean, int expect, int update) {
        return unsafe.compareAndSwapInt(bean, this.offset, expect, update);
    }

    public final void set(T bean, int newValue) {
        unsafe.putIntVolatile(bean, this.offset, newValue);
    }

    public final void lazySet(T bean, int newValue) {
        unsafe.putOrderedInt(bean, this.offset, newValue);
    }

    public final int get(T bean) {
        return unsafe.getIntVolatile(bean, this.offset);
    }

    /**
     * Atomic Reference Field Updater Implementation(Don't use in other place)
     *
     * @author Chris Liao
     * @version 1.0
     */
    public static final class AtomicReferenceFieldUpdaterImpl<T, V> extends AtomicReferenceFieldUpdater<T, V> {
        private final static UnsafeAdaptor unsafe = UnsafeAdaptorFactory.get();
        private final long offset;
        private final Class<V> fieldType;

        private AtomicReferenceFieldUpdaterImpl(long offset, Class<V> fieldType) {
            this.offset = offset;
            this.fieldType = fieldType;
        }

        public static <T, V> AtomicReferenceFieldUpdater<T, V> newUpdater(Class<T> beanClass, Class<V> fieldType, String fieldName) {
            try {
                return new AtomicReferenceFieldUpdaterImpl<T, V>(unsafe.objectFieldOffset(beanClass.getDeclaredField(fieldName)), fieldType);
            } catch (Throwable e) {
                return AtomicReferenceFieldUpdater.newUpdater(beanClass, fieldType, fieldName);
            }
        }

        public final boolean compareAndSet(T bean, V expect, V update) {
            return unsafe.compareAndSwapObject(bean, this.offset, expect, update);
        }

        public final boolean weakCompareAndSet(T bean, V expect, V update) {
            return unsafe.compareAndSwapObject(bean, this.offset, expect, update);
        }

        public final void set(T bean, V newValue) {
            unsafe.putObjectVolatile(bean, this.offset, newValue);
        }

        public final void lazySet(T bean, V newValue) {
            unsafe.putOrderedObject(bean, this.offset, newValue);
        }

        public final V get(T bean) {
            return fieldType.cast(unsafe.getObjectVolatile(bean, this.offset));
        }
    }
}