/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.impl.bak;

/**
 * Multi resource synchronize pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public class MultiResourceSynchronizePool extends BaseResourceSynchronizePool {

    //***************************************************************************************************************//
    //                                          1: Constructors                                                      //
    //***************************************************************************************************************//

    public MultiResourceSynchronizePool(int resourceSize) {
        super(resourceSize);
    }

    public MultiResourceSynchronizePool(int resourceSize, boolean fair) {
        super(resourceSize, fair);
    }
}