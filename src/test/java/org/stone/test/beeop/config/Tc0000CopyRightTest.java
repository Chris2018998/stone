/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.config;

import org.junit.jupiter.api.Test;
import org.stone.test.InitTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * print rights info
 *
 * @author Chris Liao
 */

public class Tc0000CopyRightTest {

    @Test
    public void testOnPrintRightInfo() {
        String buf = """
                *********************************************************************************
                *                                                                               *
                *                            BeeOP Test                                         *
                *                                                                               *
                *                                                     Author:Chris2018998       *
                *                                                     All rights reserved       *
                ********************************************************************************
                """;

        try {
            InitTest.setSystemOut();
            System.out.print(buf);
            assertTrue(true);
        } finally {
            InitTest.setSystemTestOut();
        }
    }
}
