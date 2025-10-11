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
import org.stone.test.beeop.objects.books.Book;
import org.stone.test.beeop.objects.books.BookMarket;


/**
 * @author Chris Liao
 */
public class Tc0010ConfigInfoLogPrintTest {

    @Test
    public void testOnConfigPrintInd() {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();

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

        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        config.setPrintConfiguration(true);

        //situation1:
        LogCollector logCollector = LogCollector.startLogCollector();
        config.check();
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".initialSize"));
        Assertions.assertTrue(logs.contains(".maxKeySize"));

        config.addConfigPrintExclusion("aliveTestTimeout");
        config.clearAllConfigPrintExclusion();
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
        config.addConfigPrintExclusion("initialSize");
        config.addConfigPrintExclusion("maxKeySize");
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".initialSize"));
        Assertions.assertFalse(logs.contains(".maxKeySize"));


        config.setObjectInterfaces(new Class[]{Book.class});
        config.setObjectInterfaceNames(new String[]{Book.class.getName()});
        config.addFactoryProperty("name", "Edition of Java world");
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".objectInterfaces"));
        Assertions.assertTrue(logs.contains(".objectInterfaceNames"));
        Assertions.assertTrue(logs.contains(".factoryProperties"));

        config.setObjectInterfaces(new Class[]{Book.class, BookMarket.class});
        config.setObjectInterfaceNames(new String[]{Book.class.getName(), BookMarket.class.getName()});
        config.addFactoryProperty("name", "Edition of Java world");
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".objectInterfaces"));
        Assertions.assertTrue(logs.contains(".objectInterfaceNames"));
        Assertions.assertTrue(logs.contains(".factoryProperties"));


        config.addConfigPrintExclusion("objectInterfaces");
        config.addConfigPrintExclusion("objectInterfaceNames");
        config.addConfigPrintExclusion("factoryProperties");
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".objectInterfaces"));
        Assertions.assertFalse(logs.contains(".objectInterfaceNames"));
        Assertions.assertFalse(logs.contains(".factoryProperties"));

        config.setObjectInterfaces(new Class[0]);
        config.setObjectInterfaceNames(new String[0]);
        logCollector = LogCollector.startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".objectInterfaces"));
        Assertions.assertFalse(logs.contains(".objectInterfaceNames"));
        Assertions.assertFalse(logs.contains(".factoryProperties"));
    }
}
