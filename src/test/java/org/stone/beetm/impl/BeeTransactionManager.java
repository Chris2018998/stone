/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetm.impl;

import javax.transaction.*;

/**
 * TransactionManager Impl
 *
 * @author Chris Liao
 * @version 1.0
 */

public class BeeTransactionManager implements TransactionManager {
    public void begin() throws NotSupportedException, SystemException {
        //@todo
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        //@todo
    }

    public int getStatus() throws SystemException {
        return 0;//@todo
    }

    public Transaction getTransaction() throws SystemException {
        return null;//@todo
    }

    public void resume(Transaction var1) throws InvalidTransactionException, IllegalStateException, SystemException {
        //@todo
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        //@todo
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        //@todo
    }

    public void setTransactionTimeout(int var1) throws SystemException {
        //@todo
    }

    public Transaction suspend() throws SystemException {
        return null;//@todo
    }
}
