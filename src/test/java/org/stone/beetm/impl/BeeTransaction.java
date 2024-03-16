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
import javax.transaction.xa.XAResource;
import java.util.List;

/**
 * Transaction Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeTransaction implements Transaction {

    //enlistResource,delistResource
    private List<XAResourceHolder> resourceHolderList;

    private List<Synchronization> synchronizationList;

    /**
     * Complete the transaction represented by this Transaction object.
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
     * @throws IllegalStateException      Thrown if the transaction in the
     *                                    target object is inactive.
     * @throws SystemException            Thrown if the transaction manager
     *                                    encounters an unexpected error condition.
     */
    public void commit() throws RollbackException,
            HeuristicMixedException, HeuristicRollbackException,
            SecurityException, IllegalStateException, SystemException {
        //@todo
    }

    /**
     * Disassociate the resource specified from the transaction associated
     * with the target Transaction object.
     *
     * @param xaRes The XAResource object associated with the resource
     *              (connection).
     * @param flag  One of the values of TMSUCCESS, TMSUSPEND, or TMFAIL.
     * @return <i>true</i> if the resource was delisted successfully; otherwise
     * <i>false</i>.
     * @throws IllegalStateException Thrown if the transaction in the
     *                               target object is inactive.
     * @throws SystemException       Thrown if the transaction manager
     *                               encounters an unexpected error condition.
     */
    public boolean delistResource(XAResource xaRes, int flag)
            throws IllegalStateException, SystemException {
        return true;//@todo
    }

    /**
     * Enlist the resource specified with the transaction associated with the
     * target Transaction object.
     *
     * @param xaRes The XAResource object associated with the resource
     *              (connection).
     * @return <i>true</i> if the resource was enlisted successfully; otherwise
     * <i>false</i>.
     * @throws RollbackException     Thrown to indicate that
     *                               the transaction has been marked for rollback only.
     * @throws IllegalStateException Thrown if the transaction in the
     *                               target object is in the prepared state or the transaction is
     *                               inactive.
     * @throws SystemException       Thrown if the transaction manager
     *                               encounters an unexpected error condition.
     */
    public boolean enlistResource(XAResource xaRes)
            throws RollbackException, IllegalStateException,
            SystemException {
        return false;//@todo
    }

    /**
     * Obtain the status of the transaction associated with the target
     * Transaction object.
     *
     * @return The transaction status. If no transaction is associated with
     * the target object, this method returns the
     * Status.NoTransaction value.
     * @throws SystemException Thrown if the transaction manager
     *                         encounters an unexpected error condition.
     */
    public int getStatus() throws SystemException {
        return 0;//@todo
    }

    /**
     * Register a synchronization object for the transaction currently
     * associated with the target object. The transction manager invokes
     * the beforeCompletion method prior to starting the two-phase transaction
     * commit process. After the transaction is completed, the transaction
     * manager invokes the afterCompletion method.
     *
     * @param sync The Synchronization object for the transaction associated
     *             with the target object.
     * @throws RollbackException     Thrown to indicate that
     *                               the transaction has been marked for rollback only.
     * @throws IllegalStateException Thrown if the transaction in the
     *                               target object is in the prepared state or the transaction is
     *                               inactive.
     * @throws SystemException       Thrown if the transaction manager
     *                               encounters an unexpected error condition.
     */
    public void registerSynchronization(Synchronization sync)
            throws RollbackException, IllegalStateException,
            SystemException {
        //@todo
    }

    /**
     * Rollback the transaction represented by this Transaction object.
     *
     * @throws IllegalStateException Thrown if the transaction in the
     *                               target object is in the prepared state or the transaction is
     *                               inactive.
     * @throws SystemException       Thrown if the transaction manager
     *                               encounters an unexpected error condition.
     */
    public void rollback() throws IllegalStateException, SystemException {
        //@todo
    }

    /**
     * Modify the transaction associated with the target object such that
     * the only possible outcome of the transaction is to roll back the
     * transaction.
     *
     * @throws IllegalStateException Thrown if the target object is
     *                               not associated with any transaction.
     * @throws SystemException       Thrown if the transaction manager
     *                               encounters an unexpected error condition.
     */
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        //@todo
    }

    private XAResourceHolder getResourceHolder(XAResource rs) {
        for (XAResourceHolder holder : resourceHolderList) {
            if (holder.getResource() == rs) return holder;
        }
        return null;
    }
}
