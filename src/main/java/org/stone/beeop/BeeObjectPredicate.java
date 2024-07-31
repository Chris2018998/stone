/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

/**
 * A predicate interface on pooled object
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface BeeObjectPredicate {

    /**
     * Tests on an exception thrown from a pooled object,if result is not null/empty,then evict the object from pool.
     *
     * @param e is an exception thrown from a pooled object
     * @return eviction cause,which can be null or empty
     */
    String evictTest(Exception e);

}
