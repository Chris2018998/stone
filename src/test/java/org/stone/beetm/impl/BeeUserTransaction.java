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
 * UserTransaction Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeUserTransaction implements UserTransaction {
    private BeeTransactionManager tm;

    public BeeUserTransaction() {
        this(new BeeTransactionManager());
    }

    public BeeUserTransaction(BeeTransactionManager tm) {
        this.tm = tm;
    }

    /**
     * Create a new transaction and associate it with the current thread.
     *
     * @throws NotSupportedException Thrown if the thread is already
     *                               associated with a transaction and the Transaction Manager
     *                               implementation does not support nested transactions.
     * @throws SystemException       Thrown if the transaction manager
     *                               encounters an unexpected error condition.
     */
    public void begin() throws NotSupportedException, SystemException {

    }

    /**
     * Complete the transaction associated with the current thread. When this
     * method completes, the thread is no longer associated with a transaction.
     *
     * @throws RollbackException          Thrown to indicate that
     *                                    the transaction has been rolled back rather than committed.
     * @throws HeuristicMixedException    Thrown to indicate that a heuristic
     *                                    decision was made and that some relevant updates have been committed
     *                                    while others have been rolled back.
     * @throws HeuristicRollbackException Thrown to indicate that a
     *                                    heuristic decision was made and that all relevant updates have been
     *                                    rolled back.
     * @throws SecurityException          Thrown to indicate that the thread is
     *                                    not allowed to commit the transaction.
     * @throws IllegalStateException      Thrown if the current thread is
     *                                    not associated with a transaction.
     * @throws SystemException            Thrown if the transaction manager
     *                                    encounters an unexpected error condition.
     */
    public void commit() throws RollbackException,
            HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {

    }

    /**
     * Roll back the transaction associated with the current thread. When this
     * method completes, the thread is no longer associated with a transaction.
     *
     * @throws SecurityException     Thrown to indicate that the thread is
     *                               not allowed to roll back the transaction.
     * @throws IllegalStateException Thrown if the current thread is
     *                               not associated with a transaction.
     * @throws SystemException       Thrown if the transaction manager
     *                               encounters an unexpected error condition.
     */
    public void rollback() throws IllegalStateException, SecurityException,
            SystemException {

    }

    /**
     * Modify the transaction associated with the current thread such that
     * the only possible outcome of the transaction is to roll back the
     * transaction.
     *
     * @throws IllegalStateException Thrown if the current thread is
     *                               not associated with a transaction.
     * @throws SystemException       Thrown if the transaction manager
     *                               encounters an unexpected error condition.
     */
    public void setRollbackOnly() throws IllegalStateException, SystemException {

    }

    /**
     * Obtain the status of the transaction associated with the current thread.
     *
     * @return The transaction status. If no transaction is associated with
     * the current thread, this method returns the Status.NoTransaction
     * value.
     * @throws SystemException Thrown if the transaction manager
     *                         encounters an unexpected error condition.
     */
    public int getStatus() throws SystemException {

    }

    /**
     * Modify the timeout value that is associated with transactions started
     * by the current thread with the begin method.
     *
     * <p> If an application has not called this method, the transaction
     * service uses some default value for the transaction timeout.
     *
     * @param seconds The value of the timeout in seconds. If the value is zero,
     *                the transaction service restores the default value. If the value
     *                is negative a SystemException is thrown.
     * @throws SystemException Thrown if the transaction manager
     *                         encounters an unexpected error condition.
     */
    public void setTransactionTimeout(int seconds) throws SystemException {

    }
}
