/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.firefly.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 */

public interface WaitChain {

    int getLength();

    int getLength(int state);

    Thread[] getThreads();

    Thread[] getThreads(int state);

    WaitNode addNode(int state);

    WaitNode addNode(int state, WaitNode node);

    boolean removeNode(WaitNode node);

}




