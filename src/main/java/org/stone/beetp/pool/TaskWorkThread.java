/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.pool;

/**
 * Task work thread
 *
 * @author Chris Liao
 * @version 1.0
 */
class TaskWorkThread extends Thread {
    volatile BaseTaskHandle currentTaskHandle;

    void interrupt(BaseTaskHandle taskHandle) {
        if (taskHandle == this.currentTaskHandle)
            this.interrupt();
    }
}
