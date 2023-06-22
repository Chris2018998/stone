/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.base.validator;

/**
 * result class type validator
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ResultNumberTypeValidator extends ResultClassTypeValidator {

    public ResultNumberTypeValidator(Class<Number> classType) {
        super(classType);
    }

    public ResultNumberTypeValidator(Class<Number> classType, Object resultOnTimeout) {
        super(classType, resultOnTimeout);
    }
}
