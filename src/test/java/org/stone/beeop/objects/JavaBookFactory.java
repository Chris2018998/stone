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
    protected String defaultKey = "JavaWorld";
    private String name;
    private String title;
    private String author;
    private String language;
    private Double price;

    public JavaBookFactory() {
        this("Edition of Java world");
    }

    public JavaBookFactory(String name) {
        this.name = name;
        this.defaultKey = "JavaWorld";
    }

    //***************************************************************************************************************//
    //               1: factory properties                                                                           //                                                                                  //
    //***************************************************************************************************************//
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    //***************************************************************************************************************//
    //               2: factory override methods                                                                     //                                                                                  //
    //***************************************************************************************************************//
    //returns default key to be pooled
    public Object getDefaultKey() {
        return defaultKey;
    }

    public void setDefaultKey(String defaultKey) {
        this.defaultKey = defaultKey;
    }

    //creates an object to pool with specified key
    public Object create(Object key) throws Exception {
        JavaBook book = new JavaBook(name);
        book.setTitle(this.title);
        book.setAuthor(this.author);
        book.setLanguage(this.language);
        book.setPrice(this.price);
        return book;
    }

    //set default to a pooled object
    public void setDefault(Object key, Object obj) {
    }

    //reset dirty properties of given object to default
    public void reset(Object key, Object obj) {
    }

    //executes alive test on an object
    public boolean isValid(Object key, Object obj, int timeout) {
        return true;
    }

    //destroy an object
    public void destroy(Object key, Object obj) {
    }
}