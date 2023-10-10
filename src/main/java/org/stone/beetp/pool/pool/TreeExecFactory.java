///*
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// *
// * Copyright(C) Chris2018998,All rights reserved.
// *
// * Project owner contact:Chris2018998@tom.com.
// *
// * Project Licensed under GNU Lesser General Public License v2.1.
// */
//
//package org.stone.beetp.pool;
//
//import org.stone.beetp.BeeTask;
//import org.stone.beetp.BeeTreeTask;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * Join Execute Factory
// *
// * @author Chris Liao
// * @version 1.0
// */
//final class TreeExecFactory extends GenericFactory {
//
//    TreeExecFactory(TaskPoolImplement pool) {
//        super(pool);
//    }
//
//    void execute(BaseHandle handle) {
//        TreeTaskHandle treeHandle = (TreeTaskHandle) handle;
//        if (treeHandle.isRoot()) beforeExecute(handle);
//
//        //2: try to split task to children tasks
//        BeeTreeTask treeTask = treeHandle.task
//        List<BeeTask> childTasks = treeHandle.g
//
//        //3: create sub children and push them to queue
//        if (childTasks != null && !childTasks.isEmpty()) {
//            int childrenSize = chisldTasks.size();
//            JoinTaskHandle root = joinHandle.getRoot();
//            if (root == null) root = joinHandle;
//            AtomicInteger completedCount = new AtomicInteger();
//            ArrayList<JoinTaskHandle> childList = new ArrayList<>(childrenSize);
//            for (BeeTask childTask : childTasks) {
//                JoinTaskHandle childHandle = new JoinTaskHandle(childTask, joinHandle, childrenSize, completedCount, joinOperator, pool, root);
//                pool.pushToExecutionQueue(childHandle);
//                childList.add(childHandle);
//            }
//            joinHandle.setChildrenList(childList);
//        } else {//execute leafed task
//            executeTask(handle);
//        }
//    }
//}