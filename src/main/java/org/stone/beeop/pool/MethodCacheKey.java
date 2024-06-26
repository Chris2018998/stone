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

/**
 * Pooled object method cache key
 *
 * @author Chris Liao
 * @version 1.0
 */

final class MethodCacheKey {
    private final String name;
    private final Class<?>[] types;

    MethodCacheKey(String name, Class<?>[] types) {
        this.name = name;
        this.types = types;
    }

    public int hashCode() {
        int result = this.name.hashCode();
        result = 31 * result + Arrays.hashCode(this.types);
        return result;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        MethodCacheKey that = (MethodCacheKey) o;
        return this.name.equals(that.name) &&
                Arrays.equals(this.types, that.types);
    }
}

