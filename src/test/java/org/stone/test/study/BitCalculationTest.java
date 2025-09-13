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
    }
}
