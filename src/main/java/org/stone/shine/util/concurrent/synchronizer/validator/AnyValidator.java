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
 * any Validator
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class AnyValidator extends BaseValidator {

    public AnyValidator() {
    }

    public AnyValidator(Object resultOnTimeout) {
        super(resultOnTimeout);
    }

    //check call result or state is whether expected
    public boolean isExpected(Object result) {
        return true;
    }
}
