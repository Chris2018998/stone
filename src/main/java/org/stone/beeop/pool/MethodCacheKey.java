/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

import java.util.Arrays;
import java.util.Objects;

/**
 * Pooled object method cache key
 *
 * @author Chris Liao
 * @version 1.0
 */

final class MethodCacheKey {
    private final int hashCode;
    private final String name;
    private final Class<?>[] types;

    MethodCacheKey(String name, Class<?>[] types) {
        this.name = name;
        this.types = types;
        this.hashCode = 31 * this.name.hashCode() + Arrays.hashCode(this.types);
    }

    public int hashCode() {
        return this.hashCode;
    }

    public boolean equals(Object o) {
        MethodCacheKey that = (MethodCacheKey) o;
        return Objects.equals(this.name, that.name) &&
                Arrays.equals(this.types, that.types);
    }
}

