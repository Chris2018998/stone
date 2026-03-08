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
import org.stone.test.beecp.objects.BeeCPHello;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * @author Chris Liao
 */
public class Tc0036ObjectSourcePoolMBeanTest {
    @Test
    public void testRegisterSuccess() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setRegisterMbeans(false);
        String poolName = "JMX-POOL";
        config.setPoolName(poolName);
        String name1 = String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", poolName);
        String name2 = String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", poolName);
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));
        }

        config.setRegisterMbeans(true);
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));
        }
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));
    }

    @Test
    public void testRegisterFail() throws Exception {
        String poolName = "JMX-POOL";
        String name1 = String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", poolName);
        String name2 = String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", poolName);
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            Assertions.assertNotNull(mBeanServer.registerMBean(new BeeCPHello(), jmxRegName1));
            Assertions.assertNotNull(mBeanServer.registerMBean(new BeeCPHello(), jmxRegName2));

            BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
            config.setPoolName(poolName);
            config.setPrintRuntimeLogs(true);
            config.setRegisterMbeans(true);

            LogCollector logCollector = LogCollector.startLogCollector();
            try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
                Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
                Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));
            }
            String logs = logCollector.endLogCollector();
            String msg1 = "BeeOP(" + poolName + ")-failed to register a MBean with name:" + name1;
            String msg2 = "BeeOP(" + poolName + ")-failed to register a MBean with name:" + name2;

            Assertions.assertTrue(logs.contains(msg1));
            Assertions.assertTrue(logs.contains(msg2));
        } finally {
            if (mBeanServer.isRegistered(jmxRegName1))
                mBeanServer.unregisterMBean(jmxRegName1);
            if (mBeanServer.isRegistered(jmxRegName2))
                mBeanServer.unregisterMBean(jmxRegName2);
        }
    }

    @Test
    public void testUnRegisterFail() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setPrintRuntimeLogs(true);
        config.setRegisterMbeans(true);
        String poolName = "JMX-POOL";
        config.setPoolName(poolName);

        String name1 = String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", poolName);
        String name2 = String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", poolName);

        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        LogCollector logCollector = LogCollector.startLogCollector();
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));

            mBeanServer.unregisterMBean(jmxRegName1);
            mBeanServer.unregisterMBean(jmxRegName2);
        }
        String logs = logCollector.endLogCollector();
        String msg1 = "BeeOP(" + poolName + ")-failed to unregister a MBean with name:" + name1;
        String msg2 = "BeeOP(" + poolName + ")-failed to unregister a MBean with name:" + name2;
        Assertions.assertTrue(logs.contains(msg1));
        Assertions.assertTrue(logs.contains(msg2));
    }
}
