/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beetp.join;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;

/**
 * Join Test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ArraySumComputeTest extends TestCase {
    public static void main(String[] args) throws Exception {
        ArraySumComputeTest test = new ArraySumComputeTest();
        test.test();
    }

    public void test() throws Exception {
        //1: create task pool
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(10);
        BeeTaskService service = new BeeTaskService(config);

        //2: create an integer array
        int sum = 0;
        int count = 100;
        int[] numbers = new int[count];
        for (int i = 0; i < count; i++) {
            numbers[i] = i;
            sum += numbers[i];
        }

        //3: create the summary task with the array
        ArraySumComputeTask task = new ArraySumComputeTask(numbers);
        //4: submit the task to pool as once task
        BeeTaskHandle<Integer> onceHandle = service.submit(task);
        //5: submit the task to pool as join task
        BeeTaskHandle<Integer> joinHandle = service.submit(task, new ArraySumJoinOperator());
        //6: get the value from handle
        int onceSum = onceHandle.get();
        int joinSum = joinHandle.get();

        //7: check result
        if (sum != onceSum) TestUtil.assertError("Once expect:%s,actual value:%s", sum, onceSum);
        if (sum != joinSum) TestUtil.assertError("Join expect:%s,actual value:%s", sum, joinSum);

        System.out.println("Success,Join Result[" + joinSum + "]is right!");
    }
}
