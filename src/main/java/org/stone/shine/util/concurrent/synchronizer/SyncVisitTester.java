/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer;

/**
 * synchronization tester on first Visit
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface SyncVisitTester {


    //interface method
    boolean allow(boolean unfair, Object curType, ObjectWaitPool pool);
}
