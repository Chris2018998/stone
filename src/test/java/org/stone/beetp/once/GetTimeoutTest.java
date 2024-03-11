/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.once;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beetp.BlockTask;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;
import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;

import java.util.concurrent.TimeUnit;

/**
 * Get Timeout test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class GetTimeoutTest extends TestCase {
    public void test() throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        TaskService service = new TaskService(config);
        TaskHandle handle = service.submit(new BlockTask());//park worker thread

        try {
            handle.get(100, TimeUnit.MILLISECONDS);
            TestUtil.assertError("Time out test Failed");
        } catch (TaskResultGetTimeoutException e) {
            //
        }
    }
}