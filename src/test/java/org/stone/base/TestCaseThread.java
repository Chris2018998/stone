/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.base;

/**
 * Test case thread
 *
 * @author chris liao
 */
public class TestCaseThread extends Thread {
    protected TestCase ownerCase;

    public TestCase getOwnerCase() {
        return ownerCase;
    }

    public void setOwnerCase(TestCase ownerCase) {
        this.ownerCase = ownerCase;
    }
}
