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
import org.stone.beetp.*;

/**
 * cancel test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskCancelTest extends TestCase {
    public void test() throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        TaskService service = new TaskService(config);

        service.submit(new BlockTask());//park worker thread
        TaskHandle commonHandle = service.submit(new HelloTask());
        commonHandle.cancel(false);
        if (!commonHandle.isCancelled()) TestUtil.assertError("Task not be cancelled");
    }
}
