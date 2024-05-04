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

import javax.transaction.xa.Xid;

/**
 * Xid Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeTransactionXid implements Xid {
    private final byte[] globalTransactionId;//generated in BeeTransaction

    private final byte[] branchQualifierId;//generated in local class

    public BeeTransactionXid(byte[] globalTransactionId) {
        this.globalTransactionId = globalTransactionId;
        this.branchQualifierId = generateBranchQualifierId();
    }

    public int getFormatId() {
        return 0;
    }

    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    public byte[] getBranchQualifier() {
        return branchQualifierId;
    }

    private byte[] generateBranchQualifierId() {
        return null;//@todo   Xid.MAXBQUALSIZE;
    }
}
