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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InvokeAllTimeoutTest extends TestCase {
    public void test() throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(2);
        BeeTaskService service = new BeeTaskService(config);

        List<BeeTask> taskList = new ArrayList<>();
        taskList.add(new HelloTask());
        taskList.add(new HelloTask());
        taskList.add(new BlockTask());

        boolean existException = false;
        List<BeeTaskHandle> handleList = service.invokeAll(taskList, 3, TimeUnit.SECONDS);
        System.out.println("handleList:"+handleList);

        for (BeeTaskHandle handle : handleList) {
            if (handle.isCancelled() || handle.isCallException()) {
                existException = true;
                break;
            }
        }

        if (!existException) TestUtil.assertError("InvokeAll timeout test failed");
    }
}