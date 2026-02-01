package org.stone.test.study;

public class BitCalculationTest {

    public static void main(String[] args) {
        int count = Integer.MAX_VALUE;
        int start = Integer.MIN_VALUE;

        final boolean a = true, b = true;
        final byte c = 0, d = 1, e = 1;
        long startTime = System.nanoTime();
        for (int i = start; i < count; i++) {
            if (c != d) {
            }
        }
        long endTime1 = System.nanoTime();
        for (int i = start; i < count; i++) {
            if ((c ^ d) == e) {
            }
        }
        long endTime2 = System.nanoTime();

        System.out.println("time1:" + (endTime1 - startTime));
        System.out.println("time2:" + (endTime2 - endTime1));
        System.out.println((1 ^ 1) == 0);
        System.out.println((0 ^ 0) == 0);
        System.out.println((1 ^ 0) == 1);
        System.out.println((0 ^ 1) == 1);

        test(1, 2);
        test(3, 4);
        test(5, 6);
        test(7, 8);
        test(100, 150);

    }
    public static void test(int a, int b) {
        int c = a | b;
        System.out.println(a == (c & a));
        System.out.println(b == (c & b));
    }
}
