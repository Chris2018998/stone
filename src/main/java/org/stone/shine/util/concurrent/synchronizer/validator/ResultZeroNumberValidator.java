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

/**
 * result class type validator
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ResultZeroNumberValidator extends ResultNumberTypeValidator {

    public ResultZeroNumberValidator(Class<Number> classType) {
        super(classType);
    }

    public ResultZeroNumberValidator(Class<Number> classType, Object resultOnTimeout) {
        super(classType, resultOnTimeout);
    }

    public final boolean isExpected(Object result) {
        if (super.isExpected(result)) {
            Number value = (Number) result;
            return value.doubleValue() == 0;
        }
        return false;
    }
}
