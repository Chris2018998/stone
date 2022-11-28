/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.base;

/**
 * Result Validator
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ResultValidator {

    Object resultOnTimeout();

    boolean validate(Object result);

}
