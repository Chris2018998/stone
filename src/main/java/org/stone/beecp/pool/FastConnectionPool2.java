/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.pool.exception.ConnectionGetForbiddenException;

import java.lang.ref.WeakReference;
import java.sql.SQLException;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;

/**
 * JDBC Connection Pool Implementation to support thread local cache
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class FastConnectionPool2 extends FastConnectionPool {
    private final ThreadLocal<WeakReference<Borrower>> threadLocal = new BorrowerThreadLocal();

    PooledConnection getPooledConnection(boolean useInputted, String username, String password, Borrower b) throws SQLException {
        if (this.poolState != POOL_READY)
            throw new ConnectionGetForbiddenException("Pool has been closed or is being cleared");

        //1: firstly, get last used connection from threadLocal if threadLocal is supported
        b = this.threadLocal.get().get();
        if (b != null) {
            PooledConnection p = b.lastUsed;
            if (p != null && p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_BORROWED)) {
                if (this.testOnBorrow(p)) return b.lastUsed = p;
                b.lastUsed = null;
            }
        }

        PooledConnection p = super.getPooledConnection(useInputted, username, password, b);
        if (b != null) {
            b.lastUsed = p;
        } else {
            this.threadLocal.set(new WeakReference<>(new Borrower(p)));
        }

        return p;
    }

    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<Borrower>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<Borrower> initialValue() {
            return new WeakReference<>(new Borrower());
        }
    }
}
