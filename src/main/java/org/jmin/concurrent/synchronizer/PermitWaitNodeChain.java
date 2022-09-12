/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.concurrent.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 */

public interface PermitWaitNodeChain {

    int getLength();

    int getLength(int state);

    Thread[] getThreads();

    Thread[] getThreads(int state);

    PermitWaitNode addNode(int state);

    PermitWaitNode addNode(int state, PermitWaitNode node);

    boolean removeNode(PermitWaitNode node);

}




