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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.base.LogCollector;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0035ObjectSourcePoolLogPrintTest {

    @Test
    public void test() throws Exception {
        //Not print ----> print logs
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setPrintRuntimeLogs(false);
        LogCollector logCollector1 = LogCollector.startLogCollector();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(logCollector1.endLogCollector().isEmpty());

            //print logs
            os.enableLogPrinter(true);
            LogCollector logCollector2 = LogCollector.startLogCollector();
            os.restart(true);
            Assertions.assertFalse(logCollector2.endLogCollector().isEmpty());

            //Not print logs
            os.enableLogPrinter(false);
            LogCollector logCollector3 = LogCollector.startLogCollector();
            os.restart(true);
            Assertions.assertTrue(logCollector3.endLogCollector().isEmpty());
        }
    }
}
