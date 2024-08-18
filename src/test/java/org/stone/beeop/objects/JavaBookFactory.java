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

import org.stone.beeop.BeeObjectFactory;

/**
 * Java Book Factory
 *
 * @author Chris Liao
 */

public class JavaBookFactory implements BeeObjectFactory {
    private final String bookName;

    public JavaBookFactory() {
        this("Edition of Java world");
    }

    public JavaBookFactory(String name) {
        this.bookName = name;
    }

    //returns default key to be pooled
    public Object getDefaultKey() {
        return "JavaWorld";
    }

    //creates an object to pool with specified key
    public Object create(Object key) throws Exception {
        return new JavaBook(bookName);
    }

    //set default to a pooled object
    public void setDefault(Object key, Object obj) throws Exception {

    }

    //reset dirty properties of given object to default
    public void reset(Object key, Object obj) throws Exception {

    }

    //executes alive test on an object
    public boolean isValid(Object key, Object obj, int timeout) throws Exception {
        return true;
    }

    //destroy an object
    public void destroy(Object key, Object obj) throws Exception {

    }
}