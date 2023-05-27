/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beetp;

/**
 * Failed task
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ExceptionTask implements BeeTask {
    public Object call() throws Exception {
        throw new Exception("Failed");
    }
}
