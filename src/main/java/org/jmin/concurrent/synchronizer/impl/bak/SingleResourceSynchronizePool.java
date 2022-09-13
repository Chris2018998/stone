/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.concurrent.synchronizer.impl.bak;

/**
 * Single resource synchronize pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SingleResourceSynchronizePool extends BaseResourceSynchronizePool {

    //***************************************************************************************************************//
    //                                          1: Constructors                                                      //
    //***************************************************************************************************************//
    private transient volatile int holdCount;

    public SingleResourceSynchronizePool(boolean fair) {
        super(1, fair);
    }


}
