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

    public boolean enlistResource(XAResource var1) throws RollbackException, IllegalStateException, SystemException {
        return true;//@todo
    }

    public boolean delistResource(XAResource var1, int var2) throws IllegalStateException, SystemException {
        return true;//@todo
    }

    public void registerSynchronization(Synchronization var1) throws RollbackException, IllegalStateException, SystemException {
        //@todo
    }

    public int getStatus() throws SystemException {
        return 0;//@todo
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        //@todo
    }

    public void rollback() throws IllegalStateException, SystemException {
        //@todo
    }

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
