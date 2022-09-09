/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 */

public interface SynchronizeNodeChain {

    int getLength();

    int getLength(int state);

    Thread[] getNodeThreads();

    Thread[] getNodeThreads(int state);

    SynchronizeNode addNode(int state);

    void addNode(int state, SynchronizeNode node);

    void removeNode(SynchronizeNode node);

}




