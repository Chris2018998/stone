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

/**
 * Base Validator
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class BaseValidator implements ResultValidator {

    private final Object resultOnTimeout;

    BaseValidator() {
        this.resultOnTimeout = null;
    }

    BaseValidator(Object resultOnTimeout) {
        this.resultOnTimeout = resultOnTimeout;
    }

    public final Object resultOnTimeout() {
        return resultOnTimeout;
    }
}
