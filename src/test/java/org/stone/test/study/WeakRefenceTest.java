package org.stone.test.study;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class WeakRefenceTest {

    //-Xmx3m
    public static void main(String[] args) {
//        testWeakReference1();
//        testWeakReferenceOnOutOfMemory();
        testThreadLocal();
    }

    private static void testWeakReference1() {
        WeakReference<Object> reference = new WeakReference<>(new Object());
        System.out.println("<testWeakReference>--before call gc():" + reference.get());
        System.gc();
        System.out.println("<testThreadLocal>--after call gc():" + reference.get());//should be null
    }

    //heap is over of memory
    private static void testWeakReferenceOnOutOfMemory() {
        Object a = new Object();
        WeakReference<Object> reference = new WeakReference<>(a);
        System.out.println("<testWeakReference>--before call gc():" + reference.get());
        try {
            List<Object> objctList = new ArrayList<>();
            for (int i = 0; i < 100000000; i++) {
                objctList.add(new Object());
            }
        } finally {
            System.out.println("<testThreadLocal>--after call gc():" + reference.get());////maybe be null
        }
    }

    private static void testThreadLocal() {
        ThreadLocal<Object> cacheLocal = new ThreadLocal<>();
        cacheLocal.set(new Object());

        try {
            List<Object> objctList = new ArrayList<>();
            for (int i = 0; i < 100000000; i++) {
                objctList.add(new Object());
            }
        } finally {
            System.out.println("<testThreadLocal>--after call gc():" + cacheLocal.get());
        }
    }
}
