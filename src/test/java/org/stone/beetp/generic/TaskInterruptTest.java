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
import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;
import org.stone.beetp.BlockTask;
import org.stone.beetp.pool.exception.TaskExecutionException;

/**
 * cancel test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskInterruptTest extends TestCase {

    public void test() throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        BeeTaskService service = new BeeTaskService(config);

        BeeTaskHandle handle = service.submit(new BlockTask());//park worker thread
        Thread.sleep(2000);//park main thread
        handle.cancel(true);

        try {
            handle.get();
        } catch (TaskExecutionException e) {
            Throwable ee = e.getCause();
            if (!(ee instanceof InterruptedException))
                TestUtil.assertError("Task interrupted test failed");
        } catch (Exception e) {
            TestUtil.assertError("Task interrupted test failed");
        }
    }
}


