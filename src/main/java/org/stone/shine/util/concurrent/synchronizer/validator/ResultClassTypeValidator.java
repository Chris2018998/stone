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

public class ResultClassTypeValidator extends BaseValidator {

    private final Class classType;

    public ResultClassTypeValidator(Class classType) {
        this(classType, null);
    }

    public ResultClassTypeValidator(Class classType, Object resultOnTimeout) {
        super(resultOnTimeout);
        this.classType = classType;
        if (classType == null) throw new IllegalArgumentException("result class type can't be null");
    }

    public boolean isExpected(Object result) {
        return classType.isInstance(result);
    }
}
