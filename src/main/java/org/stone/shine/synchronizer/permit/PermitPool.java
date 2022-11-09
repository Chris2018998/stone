/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.permit;

import org.stone.shine.synchronizer.base.ResultWaitPool;

/**
 * @author Chris Liao
 * @version 1.0
 */

public final class PermitPool extends ResultWaitPool {
    private final int permits;

    public PermitPool(int permits) {
        this(permits, false);
    }

    public PermitPool(int permits, boolean fair) {
        super(fair);
        this.permits = permits;
    }

    //available PermitPool size in pool
    public int getPermitSize() {
        return permits;
    }

}
