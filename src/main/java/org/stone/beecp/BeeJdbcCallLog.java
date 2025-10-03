/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A method call log is generated from {@link BeeJdbcCallLogCollector#startCall(int, String, Object[], String, Statement)}
 *
 * @author Chris Liao
 */
public interface BeeJdbcCallLog extends Serializable {

    int Type_Get_Connection = 1;

    int Type_Execution_SQL = 2;

    //Log type
    int getType();

    //log id
    Object getId();

    String getMethod();

    Object[] getParameters();

    long getStartTime();

    long getEndTime();

    Object getResultObject();

    Throwable getFailCause();

    String getDatasourceInfo();

    boolean isRemoved();

    boolean isProcessed();

    String getSql();

    long getSqlPreparedTime();

    Object[] getSqlPreparedParameters();

    boolean isRunningStatement();

    void cancelRunningStatement() throws SQLException;
}
