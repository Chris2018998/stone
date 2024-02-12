/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beecp;

import java.sql.Connection;
import java.sql.SQLDataException;
import java.util.concurrent.Executor;

/**
 * Interface reset connection dirty properties and provide two implementation in pool and add a new config item:resetType
 * 1) ResetByDirtyBit,which is a default to config item
 * 2) ResetByDirtyVal
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionReset {

    void onSetSchema(String newSchema) throws SQLDataException;

    void onSetCatalog(String newCatalog) throws SQLDataException;

    void onSetReadOnly(boolean newReadOnly) throws SQLDataException;

    void onSetAutoCommit(boolean newAutoCommit) throws SQLDataException;

    void onSetTransactionIsolation(String newTransactionIsolation) throws SQLDataException;

    void onsetNetworkTimeout(Executor newExecutor, int milliseconds) throws SQLDataException;

    boolean isDirty() throws SQLDataException;

    void resetConnection() throws SQLDataException;
}
