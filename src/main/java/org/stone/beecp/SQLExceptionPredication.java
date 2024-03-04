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

import java.sql.SQLException;

/**
 * Connection eviction test on an SQLException
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface SQLExceptionPredication {

    //result is not null or not empty,means a cause of eviction
    String check(SQLException e);
}


