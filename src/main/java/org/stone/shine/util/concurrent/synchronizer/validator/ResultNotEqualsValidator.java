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

import static org.stone.tools.CommonUtil.objectEquals;

/**
 * not equals compare
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ResultNotEqualsValidator extends EquivalentValidator {

    public ResultNotEqualsValidator(Object compareValue) {
        super(compareValue);
    }

    public ResultNotEqualsValidator(Object compareValue, Object resultOnTimeout) {
        super(compareValue, resultOnTimeout);
    }

    public boolean isExpected(Object result) {
        return !objectEquals(result, compareValue);
    }
}
