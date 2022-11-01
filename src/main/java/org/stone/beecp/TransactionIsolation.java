/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.sql.Connection.*;

/**
 * Transaction Isolation Level
 *
 * @author Chris Liao
 */
public final class TransactionIsolation {

    public static final String LEVEL_NONE = "NONE";

    public static final String LEVEL_READ_COMMITTED = "READ_COMMITTED";

    public static final String LEVEL_READ_UNCOMMITTED = "READ_UNCOMMITTED";

    public static final String LEVEL_REPEATABLE_READ = "REPEATABLE_READ";

    public static final String LEVEL_SERIALIZABLE = "SERIALIZABLE";

    static final String TRANS_LEVEL_CODE_LIST = TRANSACTION_NONE + "," +
            TRANSACTION_READ_COMMITTED + "," +
            TRANSACTION_READ_UNCOMMITTED + "," +
            TRANSACTION_REPEATABLE_READ + "," +
            TRANSACTION_SERIALIZABLE;

    private static final Map<String, Integer> IsolationLevelMap = new HashMap<String, Integer>(5);

    static {
        TransactionIsolation.IsolationLevelMap.put(TransactionIsolation.LEVEL_NONE, TRANSACTION_NONE);
        TransactionIsolation.IsolationLevelMap.put(TransactionIsolation.LEVEL_READ_COMMITTED, TRANSACTION_READ_COMMITTED);
        TransactionIsolation.IsolationLevelMap.put(TransactionIsolation.LEVEL_READ_UNCOMMITTED, TRANSACTION_READ_UNCOMMITTED);
        TransactionIsolation.IsolationLevelMap.put(TransactionIsolation.LEVEL_REPEATABLE_READ, TRANSACTION_REPEATABLE_READ);
        TransactionIsolation.IsolationLevelMap.put(TransactionIsolation.LEVEL_SERIALIZABLE, TRANSACTION_SERIALIZABLE);
    }

    static Integer getTransactionIsolationCode(String name) {
        return TransactionIsolation.IsolationLevelMap.get(name.toUpperCase(Locale.US));
    }

    static String getTransactionIsolationName(Integer code) {
        for (Map.Entry<String, Integer> entry : TransactionIsolation.IsolationLevelMap.entrySet()) {
            if (entry.getValue().equals(code))
                return entry.getKey();
        }
        return null;
    }
}
