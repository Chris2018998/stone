/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

import java.util.concurrent.TimeUnit;

/**
 * Task handle
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskHandle<T> {

    //***************************************************************************************************************//
    //                1: task state methods(3)                                                                       //                                                                                  //
    //***************************************************************************************************************//
    boolean isDone();

    boolean isCalling();

    boolean isCancelled();

    //***************************************************************************************************************//
    //                2: task cancel methods(1)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    boolean cancel(boolean mayInterruptIfRunning);

    //***************************************************************************************************************//
    //                3: task result getting(2)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    T get() throws BeeTaskException, InterruptedException;

    T get(long timeout, TimeUnit unit) throws BeeTaskException, InterruptedException;

}
