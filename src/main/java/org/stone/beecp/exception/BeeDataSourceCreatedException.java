/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.exception;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

/**
 * Throws this exception when failed to create {@link BeeDataSource(BeeDataSourceConfig)}.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeDataSourceCreatedException extends RuntimeException {

    public BeeDataSourceCreatedException(Throwable cause) {
        super(cause);
    }
}
