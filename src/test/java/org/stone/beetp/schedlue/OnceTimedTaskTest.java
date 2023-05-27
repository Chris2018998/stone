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
import org.stone.beetp.BeeTaskScheduledHandle;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;
import org.stone.beetp.HelloTask;

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
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        BeeTaskService service = new BeeTaskService(config);

        BeeTaskScheduledHandle handle = service.schedule(new HelloTask(), 0, TimeUnit.SECONDS);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

        if (handle.isPeriodic()) TestUtil.assertError("Once Timed Task can't be periodic");
        if (!"Hello".equals(handle.get())) TestUtil.assertError("Once Timed Task test failed");
        if (!handle.isCallResult()) TestUtil.assertError("Once Timed Task test failed");
    }
}
