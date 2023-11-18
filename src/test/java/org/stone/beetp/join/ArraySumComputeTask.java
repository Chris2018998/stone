/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beetp.join;

import org.stone.beetp.Task;

/**
 * A Join Demo task to compute sum from an integer array
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ArraySumComputeTask implements Task<Integer> {
    private final int[] numbers;

    public ArraySumComputeTask(int[] numbers) {
        this.numbers = numbers;
    }

    int[] getArray() {
        return numbers;
    }

    //task core method
    public Integer call() {
        int sum = 0;
        for (int i : numbers) sum += i;

        //System.out.println("sum:"+sum);
        return sum;
    }
}
