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
import org.stone.beetp.BeeTaskCallback;
import org.stone.beetp.BeeTaskException;

/**
 * once task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class OnceTaskHandle extends PlainTaskHandle {

    OnceTaskHandle(BeeTask task, BeeTaskCallback callback, TaskPoolImplement pool) throws BeeTaskException {
        super(task, callback, true, pool);
    }
}
