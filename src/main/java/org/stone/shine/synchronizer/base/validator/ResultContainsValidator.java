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

    public final boolean isExpect(Object result) {
        return collection.contains(result);
    }
}
