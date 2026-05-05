/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects.factory;

import org.stone.beeop.BeeObjectFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.book.TextBook;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Book Factory
 *
 * @author Chris Liao
 */
public class TextBookFactory implements BeeObjectFactory<String, Book> {
    protected String defaultKey;
    protected String bookTitle;
    protected String bookAuthor;
    protected boolean nullResultFlag;
    protected boolean bookIsValid = true;

    protected String factoryMethodPauseType = BlockWayTypes.Type_Sleep;
    protected Map<String, Long> factoryMethodPauseTimeMap = new HashMap<>(1);
    protected Map<String, Throwable> factoryMethodExceptionMap = new HashMap<>(1);

    protected String objectMethodPauseType = BlockWayTypes.Type_Sleep;
    protected Map<String, Long> objectMethodPauseTimeMap = new HashMap<>(1);
    protected Map<String, Throwable> objectMethodExceptionMap = new HashMap<>(1);

    //test properties injection
    private String factoryName;
    private String factoryCountry;

    public TextBookFactory() {
        this("Thanking in Java", "Bruce Eckel");
    }

    public TextBookFactory(String bookTitle, String bookAuthor) {
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.defaultKey = bookTitle;
    }

    @Override
    public String getDefaultKey() {
        return defaultKey;
    }

    public void setDefaultKey(String defaultKey) {
        this.defaultKey = defaultKey;
    }

    @Override
    public Book create(String key) throws Exception {
        beforeFactoryMethodCall("create");

        if (nullResultFlag) return null;
        return new TextBook(this.bookTitle, this.bookAuthor, this);
    }

    @Override
    public boolean isValid(String key, Book obj, int timeout) throws Exception {
        beforeFactoryMethodCall("isValid");
        return bookIsValid;
    }

    @Override
    public void setDefault(String key, Book obj) throws Exception {
        beforeFactoryMethodCall("setDefault");

        TextBook book = (TextBook) obj;
        book.setDefault(bookTitle, bookAuthor);
    }

    @Override
    public void reset(String key, Book obj) throws Exception {
        beforeFactoryMethodCall("reset");
        this.setDefault(key, obj);
    }

    @Override
    public void destroy(String key, Book book) throws Exception {
        beforeFactoryMethodCall("destroy");
        book.setTitle(null);
        book.setAuthor(null);
    }

    //****************************************Extra Method************************************************************//
    public void setBookIsValid(boolean bookIsValid) {
        this.bookIsValid = bookIsValid;
    }

    public void setNullResultFlag(boolean nullResultFlag) {
        this.nullResultFlag = nullResultFlag;
    }


    public String getFactoryMethodPauseType() {
        return factoryMethodPauseType;
    }

    public void setFactoryMethodPauseType(String factoryMethodPauseType) {
        this.factoryMethodPauseType = factoryMethodPauseType;
    }

    public void removeFactoryMethodPauseTime(String methodName) {
        this.factoryMethodPauseTimeMap.remove(methodName);
    }

    public void addFactoryMethodPauseTime(String methodName, Long pauseTime) {
        this.factoryMethodPauseTimeMap.put(methodName, pauseTime);
    }

    public void removeFactoryMethodException(String methodName) {
        this.factoryMethodExceptionMap.remove(methodName);
    }

    public void addFactoryMethodException(String methodName, Throwable methodException) {
        this.factoryMethodExceptionMap.put(methodName, methodException);
    }


    public String getObjectMethodPauseType() {
        return objectMethodPauseType;
    }

    public void setObjectMethodPauseType(String objectMethodPauseType) {
        this.objectMethodPauseType = objectMethodPauseType;
    }

    public void removeObjectMethodPauseTime(String methodName) {
        this.objectMethodPauseTimeMap.remove(methodName);
    }

    public void addObjectMethodPauseTime(String methodName, Long pauseTime) {
        this.objectMethodPauseTimeMap.put(methodName, pauseTime);
    }

    public void removeObjectMethodException(String methodName) {
        this.objectMethodExceptionMap.remove(methodName);
    }

    public void addObjectMethodException(String methodName, Throwable methodException) {
        this.objectMethodExceptionMap.put(methodName, methodException);
    }

    void beforeFactoryMethodCall(String methodName) throws Exception {
        //1: throw Exception
        if (factoryMethodExceptionMap != null && factoryMethodExceptionMap.containsKey(methodName)) {
            Throwable failCause = factoryMethodExceptionMap.get(methodName);
            if (failCause instanceof Exception)
                throw (Exception) failCause;
            else
                throw (Error) failCause;
        }

        //2: block current thread
        if (this.factoryMethodPauseTimeMap != null && factoryMethodPauseTimeMap.containsKey(methodName)) {
            long pauseTime = factoryMethodPauseTimeMap.get(methodName).longValue();
            if (BlockWayTypes.Type_Sleep.equals(this.factoryMethodPauseType)) {
                if (pauseTime > 0L) Thread.sleep(pauseTime);
            } else if (BlockWayTypes.Type_park.equals(factoryMethodPauseType)) {
                if (pauseTime > 0L)
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(pauseTime));
            } else {
                LockSupport.park();
            }
        }
    }

    public void beforeObjectMethodCall(String methodName) throws Exception {
        //1: throw Exception
        if (objectMethodExceptionMap != null && objectMethodExceptionMap.containsKey(methodName)) {
            Throwable failCause = objectMethodExceptionMap.get(methodName);
            if (failCause instanceof Exception)
                throw (Exception) failCause;
            else
                throw (Error) failCause;
        }

        //2: block current thread
        if (this.objectMethodPauseTimeMap != null && objectMethodPauseTimeMap.containsKey(methodName)) {
            long pauseTime = objectMethodPauseTimeMap.get(methodName).longValue();
            if (BlockWayTypes.Type_Sleep.equals(this.objectMethodPauseType)) {
                if (pauseTime > 0L) Thread.sleep(pauseTime);
            } else if (BlockWayTypes.Type_park.equals(objectMethodPauseType)) {
                if (pauseTime > 0L)
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(pauseTime));
            } else {
                LockSupport.park();
            }
        }
    }

    //****************************************Extra Method************************************************************//
    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    public String getFactoryCountry() {
        return factoryCountry;
    }

    public void setFactoryCountry(String factoryCountry) {
        this.factoryCountry = factoryCountry;
    }
}
