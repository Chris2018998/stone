/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.test.beetp.once;

import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;
import org.stone.beetp.exception.TaskRejectedException;
import org.stone.test.base.TestCase;
import org.stone.test.base.TestUtil;
import org.stone.test.beetp.HelloTask;

/**
 * task reject test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskRejectTest extends TestCase {

    public void test() throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        config.setMaxOnceTaskSize(1);
        TaskService service = new TaskService(config);

        try {
            for (int i = 0; i < 10; i++)
                service.submit(new HelloTask());
            TestUtil.assertError("Task reject test failed");
        } catch (TaskRejectedException e) {
            //
        }
    }
}
