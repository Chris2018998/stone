/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.test.beetp;

import org.stone.beetp.Task;

/**
 * park task
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BlockTask implements Task {
    private final Object syn = new Object();

    public Object call() throws Exception {
        synchronized (syn) {
            syn.wait();
        }
        return "BlockTask";
    }
}
