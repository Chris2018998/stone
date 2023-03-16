/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

import java.util.List;

/**
 * Task Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskPool {

    void init(BeeTaskPoolConfig config) throws Exception;

    BeeTaskHandle submit(BeeTask task) throws Exception;

    List<BeeTaskHandle> submit(List<BeeTask> taskList) throws Exception;

    void showdown(boolean forceCloseUsing);

    int getWorkerCount();

    int getQueueTaskCount();

    int getRunningTaskCount();

    int getCompletedTaskCount();
}
