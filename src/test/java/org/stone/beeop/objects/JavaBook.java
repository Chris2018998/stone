/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.objects;

/**
 * Book interface
 *
 * @author Chris Liao
 */

public class JavaBook implements Book {

    private final String name;

    public JavaBook(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}