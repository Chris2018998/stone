/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beetp.join;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beetp.BeeTask;
import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;

/**
 * number not join test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class NumberComputeTest2 extends TestCase {
    public static void main(String[] args) throws Exception {
        NumberComputeTest2 test2 = new NumberComputeTest2();
        test2.test();
    }

    public void test() throws Exception {
        int sum = 0;
        int count = 100;
        int[] numbers = new int[count];
        for (int i = 0; i < count; i++) {
            numbers[i] = i;
            sum = sum + i;
        }

        ArraySumComputeTask task = new ArraySumComputeTask(numbers);
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(10);
        BeeTaskService service = new BeeTaskService(config);

        BeeTaskHandle<Integer> handle = service.submit(task, new ArraySumJoinOperator() {
            public BeeTask<Integer>[] split(BeeTask task) {
                return null;
            }

            public Integer join(BeeTaskHandle<Integer>[] subTaskHandles) {
                return 0;
            }
        });
        if (sum != handle.get()) TestUtil.assertError("Join expect:%s,actual value:%s", sum, handle.get());
    }
}
