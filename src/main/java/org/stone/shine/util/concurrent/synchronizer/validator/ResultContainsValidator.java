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

import java.util.Collection;

/**
 * Collection validator
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ResultContainsValidator extends CollectionValidator {

    public ResultContainsValidator(Collection collection) {
        super(collection);
    }

    public ResultContainsValidator(Collection collection, Object resultOnTimeout) {
        super(collection, resultOnTimeout);
    }

    public boolean isExpected(Object result) {
        return collection.contains(result);
    }
}
