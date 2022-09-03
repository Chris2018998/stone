/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop;

/**
 * configuration exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfigException extends RuntimeException {

    public BeeObjectSourceConfigException(String s) {
        super(s);
    }

    public BeeObjectSourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
