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
public class Tc0032ObjectSourcePoolMBeanTest {

    @Test
    public void testRegisterMXBean() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        String poolName = "JMX-BEEOP-POOL";
        ObjectName jmxRegName1 = new ObjectName(String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", poolName));
        ObjectName jmxRegName2 = new ObjectName(String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", poolName));

        //1:register bean/unRegister
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setRegisterMbeans(true);
        config.setPoolName(poolName);
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));
        }
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));

        //2: not register MXBean
        config.setRegisterMbeans(false);
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));
        }
    }

    @Test
    public void testRegisterMXBeaFailure() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        String poolName = "JMX-BEEOP-POOL";
        String name1 = String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", poolName);
        String name2 = String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", poolName);
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);

        try {
            mBeanServer.registerMBean(new BeeCPHello(), jmxRegName1);//register a MXBean with name1 before pool startup
            mBeanServer.registerMBean(new BeeCPHello(), jmxRegName2);//register a MXBean with name2 before pool startup

            BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
            config.setPoolName(poolName);
            config.setPrintRuntimeLogs(true);
            config.setRegisterMbeans(true);
            LogCollector logCollector = LogCollector.startLogCollector();
            try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
                Assertions.assertNotNull(ignored);
            }

            String logs = logCollector.endLogCollector();
            Assertions.assertTrue(logs.contains("BeeOP(" + poolName + ")-failed to register a MBean with name:" + name1));
            Assertions.assertTrue(logs.contains("BeeOP(" + poolName + ")-failed to register a MBean with name:" + name2));
        } finally {
            if (mBeanServer.isRegistered(jmxRegName1))
                mBeanServer.unregisterMBean(jmxRegName1);
            if (mBeanServer.isRegistered(jmxRegName2))
                mBeanServer.unregisterMBean(jmxRegName2);
        }
    }

    @Test
    public void testUnRegisterMXBeaFailure() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        String poolName = "JMX-BEEOP-POOL";
        String name1 = String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", poolName);
        String name2 = String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", poolName);
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);

        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setPrintRuntimeLogs(true);
        config.setRegisterMbeans(true);
        config.setPoolName(poolName);
        LogCollector logCollector = LogCollector.startLogCollector();
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));

            mBeanServer.unregisterMBean(jmxRegName1);
            mBeanServer.unregisterMBean(jmxRegName2);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("BeeOP(" + poolName + ")-failed to unregister a MBean with name:" + name1));
        Assertions.assertTrue(logs.contains("BeeOP(" + poolName + ")-failed to unregister a MBean with name:" + name2));
    }
}
