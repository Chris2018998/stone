/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp;

import java.util.List;

/**
 * A view object represent pool termination info contains some tasks cancelled list
 *
 * @author Chris Liao
 * @version 1.0
 */
public record TaskPoolTerminatedVo(List<Task<?>> onceTaskList, List<Task<?>> scheduledTaskList,
                                   List<Task<?>> joinTaskList, List<TreeLayerTask<?>> treeTaskList) {
}
