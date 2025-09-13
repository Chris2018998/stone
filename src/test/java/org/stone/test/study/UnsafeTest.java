package org.stone.test.study;

import jdk.internal.misc.Unsafe;

public class UnsafeTest {
    private String age;

    public static void main(String[] args) {
        Unsafe unsafe = Unsafe.getUnsafe();
        long offset = unsafe.objectFieldOffset(UnsafeTest.class, "age");

        UnsafeTest test = new UnsafeTest();
        System.out.println(unsafe.compareAndExchangeReference(test, offset, null, "safdsf"));
    }
}
