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
import org.stone.beetp.*;
import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InvokeAnyTimeoutTest extends TestCase {
    public void test() throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(2);
        BeeTaskService service = new BeeTaskService(config);

        List<BeeTask> taskList = new ArrayList<>(3);
        taskList.add(new ExceptionTask());
        taskList.add(new ExceptionTask());
        taskList.add(new BlockTask());
        try {
            service.invokeAny(taskList, 2, TimeUnit.SECONDS);
            TestUtil.assertError("Invoke any task timeout failed");
        } catch (TaskResultGetTimeoutException e) {
            //
        }
    }
}