/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent;

import org.stone.shine.util.concurrent.synchronizer.base.ResultWaitPool;

/**
 * Phaser Impl By Wait Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Phaser {
    private final ResultWaitPool waitPool;

    public Phaser() {
        this.waitPool = new ResultWaitPool();
    }
}
