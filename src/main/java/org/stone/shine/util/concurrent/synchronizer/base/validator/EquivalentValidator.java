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
 * Equivalent validator
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class EquivalentValidator extends BaseValidator {

    protected final Object compareValue;

    EquivalentValidator(Object compareValue) {
        this(compareValue, null);
    }

    EquivalentValidator(Object compareValue, Object resultOnTimeout) {
        super(resultOnTimeout);
        this.compareValue = compareValue;
    }
}
