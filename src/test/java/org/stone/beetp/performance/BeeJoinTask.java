package org.stone.beetp.performance;

import org.stone.beetp.Task;

public class BeeJoinTask implements Task<Integer> {
    private final int[] numbers;

    public BeeJoinTask(int[] numbers) {
        this.numbers = numbers;
    }

    public int[] getArray() {
        return numbers;
    }

    //task core method
    public Integer call() {
        int sum = 0;
        for (int i : numbers) sum += i;
        return sum;
    }
}