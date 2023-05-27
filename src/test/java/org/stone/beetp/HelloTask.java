/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beetp;

/**
 * hello task
 *
 * @author Chris Liao
 * @version 1.0
 */
public class HelloTask implements BeeTask {
    public Object call() {
        return "Hello";
    }
}
