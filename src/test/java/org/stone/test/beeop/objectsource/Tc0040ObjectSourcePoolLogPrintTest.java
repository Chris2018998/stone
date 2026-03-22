/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objectsource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectFactory;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.base.LogCollector;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0040ObjectSourcePoolLogPrintTest {

    @Test
    public void test() throws Exception {
        //Not print ----> print logs
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> factory = config.getObjectFactory();
        config.setPrintRuntimeLogs(false);
        String defaultKey = factory.getDefaultKey();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            //1:disabled state
            LogCollector logCollector1 = LogCollector.startLogCollector();
            os.restart(true);
            Assertions.assertTrue(logCollector1.endLogCollector().isEmpty());

            //2:enable log printer
            os.enableLogPrinter(true);
            LogCollector logCollector2 = LogCollector.startLogCollector();
            os.restart(true);
            Assertions.assertFalse(logCollector2.endLogCollector().isEmpty());

            //3:disabled log printer of key
            LogCollector logCollector3 = LogCollector.startLogCollector();
            os.clearKeyObjects(defaultKey);
            Assertions.assertTrue(logCollector3.endLogCollector().isEmpty());

            //4: enable log printer of key
            os.enableLogPrinter(defaultKey, true);
            LogCollector logCollector4 = LogCollector.startLogCollector();
            os.clearKeyObjects(defaultKey);
            Assertions.assertFalse(logCollector4.endLogCollector().isEmpty());
        }
    }
}
