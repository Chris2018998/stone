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

abstract class CollectionValidator extends BaseValidator {

    protected final Collection collection;

    CollectionValidator(Collection collection) {
        this(collection, null);
    }

    CollectionValidator(Collection collection, Object resultOnTimeout) {
        super(resultOnTimeout);
        this.collection = collection;
        if (collection == null) throw new IllegalArgumentException("collection can't be null");
    }
}
