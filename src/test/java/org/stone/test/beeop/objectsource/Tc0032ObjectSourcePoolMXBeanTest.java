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
import org.stone.test.beecp.objects.BeeCPHello;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import java.lang.management.ManagementFactory;

/**
 * @author Chris Liao
 */
public class Tc0032ObjectSourcePoolMXBeanTest {

    @Test
    public void testRegisterMXBean() throws Exception {
        String poolName = "JMX-BEEOP-POOL";
        ObjectName jmxRegName1 = new ObjectName(String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", poolName));
        ObjectName jmxRegName2 = new ObjectName(String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", poolName));

        //1:register bean/unRegister
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> objectFactory = config.getObjectFactory();

        config.setRegisterMbeans(true);
        config.setPoolName(poolName);
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));

            //1.1:attribute: KeyNames (getKeyNames())
            boolean existDefaultKey = false;
            for (String name : (String[]) mBeanServer.getAttribute(jmxRegName2, "KeyNames")) {
                if (name.equals(objectFactory.getDefaultKey())) {
                    existDefaultKey = true;
                    break;
                }
            }
            Assertions.assertTrue(existDefaultKey);

            //1.2: get PoolMonitorVo
            CompositeDataSupport poolMonitorVo = (CompositeDataSupport) mBeanServer.getAttribute(jmxRegName2, "PoolMonitorVo");
            Assertions.assertEquals(poolName, poolMonitorVo.get("poolName"));

            //1.3: method call: getKeyMonitorVoByName
            CompositeDataSupport attrKeyMonitorVoByName = (CompositeDataSupport) mBeanServer.invoke(jmxRegName2, "getKeyMonitorVoByName", new Object[]{objectFactory.getDefaultKey()}, new String[]{"java.lang.String"});
            Assertions.assertEquals(objectFactory.getDefaultKey(), attrKeyMonitorVoByName.get("keyName"));
            Assertions.assertEquals(objectFactory.getDefaultKey(), attrKeyMonitorVoByName.get("keyName"));
            Assertions.assertEquals(Boolean.FALSE, attrKeyMonitorVoByName.get("enabledLogPrinter"));
            Assertions.assertEquals(Boolean.FALSE, attrKeyMonitorVoByName.get("enabledLogCache"));

            mBeanServer.invoke(jmxRegName2, "enableKeyLogPrinterByName", new Object[]{objectFactory.getDefaultKey(), Boolean.TRUE}, new String[]{"java.lang.String", "boolean"});
            mBeanServer.invoke(jmxRegName2, "enableKeyLogCacheByName", new Object[]{objectFactory.getDefaultKey(), Boolean.TRUE}, new String[]{"java.lang.String", "boolean"});
            attrKeyMonitorVoByName = (CompositeDataSupport) mBeanServer.invoke(jmxRegName2, "getKeyMonitorVoByName", new Object[]{objectFactory.getDefaultKey()}, new String[]{"java.lang.String"});
            Assertions.assertEquals(Boolean.TRUE, attrKeyMonitorVoByName.get("enabledLogPrinter"));
            Assertions.assertEquals(Boolean.TRUE, attrKeyMonitorVoByName.get("enabledLogCache"));

            //1.4：invalid key test
            String notRegisteredKeyName = "Test Key";
            CompositeDataSupport attrKeyMonitorVoByName2 = (CompositeDataSupport) mBeanServer.invoke(jmxRegName2, "getKeyMonitorVoByName", new Object[]{notRegisteredKeyName}, new String[]{"java.lang.String"});
            Assertions.assertNull(attrKeyMonitorVoByName2);
            mBeanServer.invoke(jmxRegName2, "enableKeyLogPrinterByName", new Object[]{notRegisteredKeyName, Boolean.TRUE}, new String[]{"java.lang.String", "boolean"});
            mBeanServer.invoke(jmxRegName2, "enableKeyLogCacheByName", new Object[]{notRegisteredKeyName, Boolean.TRUE}, new String[]{"java.lang.String", "boolean"});
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
