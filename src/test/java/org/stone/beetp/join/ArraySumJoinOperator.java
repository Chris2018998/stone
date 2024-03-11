/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beetp.join;

import org.stone.beetp.Task;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskJoinOperator;

/**
 * Join Operator Implement
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ArraySumJoinOperator implements TaskJoinOperator<Integer> {

    //if the length of array is greater than 10,split it to two parts and create two sub tasks based on them
    public Task<Integer>[] split(Task task) {
        int[] array = ((ArraySumComputeTask) task).getArray();
        int arrayLen = array.length;

        if (arrayLen > 10) {//length is greater than 10
            //1: create two sub arrays
            int len1 = arrayLen / 2;
            int len2 = arrayLen - len1;
            int[] array1 = new int[len1];
            int[] array2 = new int[len2];

            //2: copy value from parent array
            for (int i = 0; i < len1; i++)
                array1[i] = array[i];
            for (int i = 0; i < len2; i++)
                array2[i] = array[i + len1];

            //4: return sub task list(bound to parent task)
            Task<Integer>[] subTaskArray = new Task[2];
            subTaskArray[0] = new ArraySumComputeTask(array1);
            subTaskArray[1] = new ArraySumComputeTask(array2);
            return subTaskArray;
        }

        return null;//return null,the parameter task will be executed in pool
    }

    //sum the computed value from children tasks
    //sum the computed value from children tasks
    public Integer join(TaskHandle<Integer>[] children) {
        int sum = 0;
        for (TaskHandle<Integer> handle : children) {
            try {
                sum += handle.get();
            } catch (Exception e) {
            }
        }
        return sum;
    }
}
