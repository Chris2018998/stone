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
import org.stone.beetp.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.TaskStates.TASK_CALL_RESULT;

/**
 * OnceTimedTaskTest
 *
 * @author Chris Liao
 * @version 1.0
 */
public class OnceTimedTaskCallbackTest extends TestCase {
    public void test() throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        TaskService service = new TaskService(config);

        CallBackImpl callback = new CallBackImpl();
        TaskScheduledHandle handle = service.schedule(new HelloTask(), 0, TimeUnit.SECONDS, callback);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

        if (handle.isPeriodic()) TestUtil.assertError("Once Timed Task can't be periodic");
        if (!"Hello".equals(handle.get())) TestUtil.assertError("Once Timed Task test failed");
        if (handle.getState() != TASK_CALL_RESULT) TestUtil.assertError("Once Timed Task test failed");
        if (!callback.beforeInd || !callback.onCallDoneInd) TestUtil.assertError("Call back test Failed");
        if (!"Hello".equals(callback.result)) TestUtil.assertError("Call back test Failed");
    }

    private static class CallBackImpl implements TaskCallback {
        private boolean beforeInd;
        private boolean onCallDoneInd;
        private Object result;

        public void beforeCall(TaskHandle handle) {
            beforeInd = true;
        }

        public void afterCall(int doneCode, Object doneResp, TaskHandle handle) {
            onCallDoneInd = true;
            result = doneResp;
        }
    }
}
