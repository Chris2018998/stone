package org.stone.beetp.performance;

import java.util.concurrent.RecursiveTask;

public class JDKJoinTask extends RecursiveTask<Integer> {
    private int[] array;

    public JDKJoinTask(int[] array) {
        this.array = array;
    }

    protected Integer compute() {
        int arrayLen = array.length;
        if (arrayLen <= 10) {
            int sum = 0;
            for (int i : array) sum += i;
            return sum;
        } else { // 否则继续拆分，递归调用
            int len1 = arrayLen / 2;
            int len2 = arrayLen - len1;
            int[] array1 = new int[len1];
            int[] array2 = new int[len2];

            //2: copy value from parent array
            for (int i = 0; i < len1; i++)
                array1[i] = array[i];
            for (int i = 0; i < len2; i++)
                array2[i] = array[i + len1];

            JDKJoinTask taskLeft = new JDKJoinTask(array1);
            JDKJoinTask taskRight = new JDKJoinTask(array2);
            taskLeft.fork();
            taskRight.fork();
            return taskLeft.join() + taskRight.join();
        }
    }
}