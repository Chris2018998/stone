/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.schedlue;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beetp.HelloTask;
import org.stone.beetp.TaskScheduledHandle;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * OnceTimedTaskTest
 *
 * @author Chris Liao
 * @version 1.0
 */
public class OnceTimedTaskTest extends TestCase {
    public void test() throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        TaskService service = new TaskService(config);

        TaskScheduledHandle handle = service.schedule(new HelloTask(), 0, TimeUnit.SECONDS);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

        if (handle.isPeriodic()) TestUtil.assertError("Once Timed Task can't be periodic");
        if (!"Hello".equals(handle.get())) TestUtil.assertError("Once Timed Task test failed");
        if (!handle.isSuccessful()) TestUtil.assertError("Once Timed Task test failed");
    }
}
