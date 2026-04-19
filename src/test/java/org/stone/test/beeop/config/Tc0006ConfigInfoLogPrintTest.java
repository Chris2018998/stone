/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * <p>
 * Copyright(C) Chris2018998,All rights reserved.
 * <p>
 * Project owner contact:Chris2018998@tom.com.
 * <p>
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.base.LogCollector;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0006ConfigInfoLogPrintTest {

    @Test
    public void testOnConfigPrintInd() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();

        //situation1: not print config
        config.setPrintConfiguration(false);//test point
        LogCollector logCollector = LogCollector.startLogCollector();
        config.check();
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.isEmpty());

        //situation2: print config items
        config.setPrintConfiguration(true);//test point
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.isEmpty());
    }

    @Test
    public void testOnExclusionConfigItems() {

        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setPrintConfiguration(true);

        //situation1:
        LogCollector logCollector = LogCollector.startLogCollector();
        config.check();
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".initialSize"));
        Assertions.assertTrue(logs.contains(".maxKeySize"));

        config.addExclusionNameOfPrint("aliveTestTimeout");
        config.clearExclusionListOfPrint();
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".initialSize"));
        Assertions.assertTrue(logs.contains(".maxKeySize"));

        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".initialSize"));
        Assertions.assertTrue(logs.contains(".maxKeySize"));

        //situation2:
        config.addExclusionNameOfPrint("initialSize");
        config.addExclusionNameOfPrint("maxKeySize");
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".initialSize"));
        Assertions.assertFalse(logs.contains(".maxKeySize"));

        config.addObjectFactoryProperty("name", "Edition of Java world");
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".objectFactoryProperties"));

        config.addObjectFactoryProperty("name", "Edition of Java world");
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".objectFactoryProperties"));

        config.addExclusionNameOfPrint("objectFactoryProperties");
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".objectFactoryProperties"));

        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".objectFactoryProperties"));
    }
}
