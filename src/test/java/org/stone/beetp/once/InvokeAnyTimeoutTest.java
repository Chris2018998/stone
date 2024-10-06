///*
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// *
// * Copyright(C) Chris2018998,All rights reserved.
// *
// * Project owner contact:Chris2018998@tom.com.
// *
// * Project Licensed under GNU Lesser General Public License v2.1.
// */
//package org.stone.beetp.once;
//
//import org.stone.base.TestCase;
//import org.stone.base.TestUtil;
//import org.stone.beetp.*;
//import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//public class InvokeAnyTimeoutTest extends TestCase {
//    public void test() throws Exception {
//        TaskServiceConfig config = new TaskServiceConfig();
//        TaskService service = new TaskService(config);
//
//        List<Task> taskList = new ArrayList<>(3);
//        taskList.add(new ExceptionTask());
//        taskList.add(new ExceptionTask());
//        taskList.add(new BlockTask());
//        try {
//            service.invokeAny(taskList, 100, TimeUnit.MILLISECONDS);
//            TestUtil.assertError("Invoke any task timeout failed");
//        } catch (TaskResultGetTimeoutException e) {
//            //
//        }
//    }
//}