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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Call back test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CallbackTest extends TestCase {

    public void test() throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        BeeTaskService service = new BeeTaskService(config);

        MyCallBack back = new MyCallBack();
        service.submit(new HelloTask(), back);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

        if (!back.beforeInd || !back.onCallDoneInd) TestUtil.assertError("Call back test Failed");
        if (!"Hello".equals(back.result)) TestUtil.assertError("Call back test Failed");
    }

    private static class MyCallBack implements BeeTaskCallback {
        private boolean beforeInd;
        private boolean onCallDoneInd;
        private Object result;

        public void beforeCall(BeeTaskHandle handle) {
            beforeInd = true;
        }

        public void afterCall(int doneCode, Object doneResp, BeeTaskHandle handle) {
            onCallDoneInd = true;
            result = doneResp;
        }
    }
}
