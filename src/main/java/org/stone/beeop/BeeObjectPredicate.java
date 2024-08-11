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
 * A predicate interface for pooled object
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface BeeObjectPredicate {

    /**
     * do test on an exception thrown from an object
     *
     * @param e is an exception thrown from a method invocation
     * @return return a string represents eviction cause,if it is not null and not empty,pool removes target object
     */
    String evictTest(Exception e);

}
