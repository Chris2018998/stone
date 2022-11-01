/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

/**
 * Pool JMX Bean interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ConnectionPoolJmxBean {

    //return current size(using +idle)
    int getTotalSize();

    //return idle connection size
    int getIdleSize();

    //return using connection size
    int getUsingSize();

    //return synchronizer size taken from semaphore
    int getSemaphoreAcquiredSize();

    //return waiting size to take semaphore synchronizer
    int getSemaphoreWaitingSize();

    //return waiter size for transferred connection
    int getTransferWaitingSize();

    //set pool info debug switch
    void setPrintRuntimeLog(boolean indicator);

}

