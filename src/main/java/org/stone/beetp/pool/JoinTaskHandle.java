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

import org.stone.beetp.BeeTask;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * generic task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class JoinTaskHandle extends OnceTaskHandle {
    private BeeTask task;
    private JoinTaskHandle parent;
    private AtomicInteger unCompleteSize;

    JoinTaskHandle(BeeTask task, JoinTaskHandle parent, int childrenSize, TaskPoolImplement pool, TaskExecFactory factory) {
        super(task, null, pool, factory);

        this.task = task;
        this.parent = parent;
        this.unCompleteSize = new AtomicInteger(childrenSize);
    }

    //call by children task
    public void decrementUnCompleteSize() {
        do {
            int currentSize = unCompleteSize.get();
            if (currentSize == 0) return;
            if (unCompleteSize.compareAndSet(currentSize, currentSize - 1)) {
                if (currentSize == 1) {
                    //@todo to join all result from children//
                }
                return;
            }
        } while (true);
    }
}
