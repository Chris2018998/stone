/// *
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
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class InvokeAnyNormalTest extends TestCase {
//    public void test() throws Exception {
//        TaskServiceConfig config = new TaskServiceConfig();
//        TaskService service = new TaskService(config);
//
//        List<Task<?>> taskList = new ArrayList<>();
//        taskList.add(new ExceptionTask());
//        taskList.add(new ExceptionTask());
//        taskList.add(new HelloTask());
//        TaskHandle handle = service.invokeAny(taskList);
//        if (handle.isFailed()) TestUtil.assertError("InvokeAny test failed");
//    }
//}