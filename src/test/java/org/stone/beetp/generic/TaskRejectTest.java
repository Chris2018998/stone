/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.generic;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;
import org.stone.beetp.HelloTask;
import org.stone.beetp.pool.exception.TaskRejectedException;

/**
 * task reject test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskRejectTest extends TestCase {

    public void test() throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        config.setTaskMaxSize(1);
        BeeTaskService service = new BeeTaskService(config);

        try {
            for (int i = 0; i < 10; i++)
                service.submit(new HelloTask());
            TestUtil.assertError("Task reject test failed");
        } catch (TaskRejectedException e) {
            //
        }
    }
}