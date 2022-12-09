/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.base.validator;

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
    public boolean isExpect(Object result) {
        return true;
    }
}
