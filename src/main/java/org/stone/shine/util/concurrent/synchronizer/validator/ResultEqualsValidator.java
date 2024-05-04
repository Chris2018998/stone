/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer.validator;

import org.stone.shine.util.concurrent.synchronizer.ResultValidator;

import java.util.Objects;

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

    public boolean isExpected(Object result) {
        return Objects.equals(compareValue, result);
    }
}
