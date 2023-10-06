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
import org.stone.beetp.BeeTaskPoolMonitorVo;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;

/**
 * Join task count test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class JoinTaskCountTest extends TestCase {
    public static void main(String[] args) throws Exception {
        JoinTaskCountTest test = new JoinTaskCountTest();
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
        //4: submit the task to pool as join task
        BeeTaskHandle<Integer> joinHandle = service.submit(task, new ArraySumJoinOperator());
        //5: get the value from handle
        int joinSum = joinHandle.get();

        //6: check result
        if (sum != joinSum) TestUtil.assertError("Join expect:%s,actual value:%s", sum, joinSum);

        //7:test task count about running,holding,completed
        BeeTaskPoolMonitorVo monitorVo = service.getPoolMonitorVo();

        if (monitorVo.getTaskHoldingCount() != 0)
            TestUtil.assertError("Pool holding count is not equal 0");
        if (monitorVo.getTaskRunningCount() != 0)
            TestUtil.assertError("Pool running count is not equal 0");
        if (monitorVo.getTaskCompletedCount() != 1)
            TestUtil.assertError("Pool completed count is not equal 1");
    }
}
