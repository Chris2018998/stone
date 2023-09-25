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

import java.util.List;

/**
 * Join task tool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskJoinOperator<E> {

    //when a join task take out from queue,need try to split,if exists children then push them to queue,if no,execute the task
    List<BeeTaskJoinHandle> trySplit(BeeTask task);

    //join children to a result
    E join(List<BeeTaskJoinHandle> children);

}
