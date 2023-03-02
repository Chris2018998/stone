/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop.pool;

import org.stone.beeop.BeeObjectHandle;


/**
 * object Handle implement
 *
 * @author Chris Liao
 * @version 1.0
 */
class ObjectHandleFactory {
    BeeObjectHandle createHandle(PooledObject p, ObjectBorrower b) {
        b.lastUsed = p;
        return new ObjectBaseHandle(p);
    }
}
