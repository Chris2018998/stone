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

import org.stone.shine.util.concurrent.synchronizer.base.ResultValidator;

import static org.stone.tools.CommonUtil.objectEquals;

/**
 * equals compare
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ResultEqualsValidator extends EquivalentValidator {
    //default validator
    public static final ResultValidator BOOL_EQU_VALIDATOR = new ResultEqualsValidator(Boolean.TRUE, Boolean.FALSE);

    public ResultEqualsValidator(Object compareValue) {
        super(compareValue);
    }

    public ResultEqualsValidator(Object compareValue, Object resultOnTimeout) {
        super(compareValue, resultOnTimeout);
    }

    public final boolean isExpected(Object result) {
        return objectEquals(compareValue, result);
    }
}
