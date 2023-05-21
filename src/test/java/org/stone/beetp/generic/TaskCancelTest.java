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

/**
 * cancel test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskCancelTest extends TestCase {
    public void test() throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        BeeTaskService service = new BeeTaskService(config);

        service.submit(new BlockTask());//block worker thread
        BeeTaskHandle commonHandle = service.submit(new CommonTask());
        commonHandle.cancel(false);
        if (!commonHandle.isCancelled()) TestUtil.assertError("Task not be cancelled");
    }
}
