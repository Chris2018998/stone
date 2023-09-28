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

import org.stone.beetp.BeeTaskHandle;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * generic task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class JoinTaskHandle implements BeeTaskHandle {
    private JoinTaskHandle parent;

    private AtomicInteger unCompleteSize;

    public JoinTaskHandle(JoinTaskHandle parent, int childrenSize) {
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
