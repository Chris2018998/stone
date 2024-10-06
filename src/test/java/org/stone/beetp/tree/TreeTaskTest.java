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
import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;

public class TreeTaskTest extends TestCase {

    public static void main(String[] args) throws Exception {
        TreeTaskTest test = new TreeTaskTest();
        test.test();
    }

    public void test() throws Exception {
        //1: create task pool
        TaskServiceConfig config = new TaskServiceConfig();
        TaskService service = new TaskService(config);

        //3: create the summary task with the array
        RootSumTask task = new RootSumTask();
        //4: submit the task to pool as once task
        TaskHandle<Integer> treeHandle = service.submit(task);
        //6: get the value from handle
        int sum = treeHandle.get();

        if (sum != 10) TestUtil.assertError("tree expect:%s,actual value:%s", sum, 10);
        System.out.println("Success,tree sum result[" + sum + "]is right!");
    }
}
