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

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * XAResource Holder
 *
 * @author Chris Liao
 * @version 1.0
 */
public class XAResourceHolder {
    private Xid xid;

    private XAResource resource;

    public XAResourceHolder(Xid xid, XAResource resource) {
        this.xid = xid;
        this.resource = resource;
    }

    public Xid getXid() {
        return xid;
    }

    public XAResource getResource() {
        return resource;
    }

}
