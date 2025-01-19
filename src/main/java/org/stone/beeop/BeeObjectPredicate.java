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
 * Object predicate interface.
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface BeeObjectPredicate {

    /**
     * Test a Exception thrown from an object,test result determine object whether evicted from pool.
     *
     * @param e thrown from a working object
     * @return a string as eviction reason,but it is null or empty,not evict target object
     */
    String evictTest(Exception e);

}
