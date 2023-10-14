/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.tree;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.BeeTaskPoolMonitorVo;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;

public class TreeExceptionTest extends TestCase {

    public static void main(String[] args) throws Exception {
        TreeExceptionTest test = new TreeExceptionTest();
        test.test();
    }

    public void test() throws Exception {
        //1: create task pool
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(10);
        BeeTaskService service = new BeeTaskService(config);

        try {
            //3: create the summary task with the array
            RootExceptionTask task = new RootExceptionTask();
            //4: submit the task to pool as once task
            BeeTaskHandle<Integer> treeHandle = service.submit(task);
            //6: get the value from handle
            treeHandle.get();

            TestUtil.assertError("Tree exception test failed");
        } catch (Exception e) {
            BeeTaskPoolMonitorVo vo = service.getPoolMonitorVo();
            if (vo.getTaskCompletedCount() != 1)
                TestUtil.assertError("tree expect:%s,actual value:%s", 1, vo.getTaskCompletedCount());
            if (vo.getTaskRunningCount() != 0)
                TestUtil.assertError("tree expect:%s,actual value:%s", 0, vo.getTaskRunningCount());
            if (vo.getTaskHoldingCount() != 0)
                TestUtil.assertError("tree expect:%s,actual value:%s", 0, vo.getTaskHoldingCount());
        }
    }
}
