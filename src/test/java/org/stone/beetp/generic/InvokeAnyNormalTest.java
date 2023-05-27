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

public class InvokeAnyNormalTest extends TestCase {
    public void test() throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        BeeTaskService service = new BeeTaskService(config);

        List<BeeTask> taskList = new ArrayList();
        taskList.add(new FailedTask());
        taskList.add(new FailedTask());
        taskList.add(new CommonTask());
        BeeTaskHandle handle = service.invokeAny(taskList);
        if (handle.isCallException()) TestUtil.assertError("InvokeAny test failed");
    }
}