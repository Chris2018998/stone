/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects.book;

/**
 * Book borrow info
 *
 * @author Chris Liao
 */
public interface BookBorrowInfo {

    String getBorrower() throws Exception;

    void setBorrower(String borrower) throws Exception;

    long getBorrowedTime() throws Exception;

    void setBorrowedTime(long borrowedTime) throws Exception;
}
