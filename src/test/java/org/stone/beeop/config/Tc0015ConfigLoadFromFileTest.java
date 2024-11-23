/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beeop.BeeObjectSourceConfig;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

import static org.stone.base.TestUtil.getClassPathFileAbsolutePath;
import static org.stone.tools.CommonUtil.loadPropertiesFromClassPathFile;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Tc0015ConfigLoadFromFileTest extends TestCase {
    private final String filename = "beeop/config2.properties";

    /********************************************Constructor**************************************************/

    public void testOnCorrectFile() throws Exception {
        String classFilename = "cp:" + filename;
        Assert.assertTrue(check(new BeeObjectSourceConfig(classFilename)));//classpath
        Assert.assertTrue(check(new BeeObjectSourceConfig(getClassPathFileAbsolutePath(filename))));//from file
        Assert.assertTrue(check(new BeeObjectSourceConfig(loadPropertiesFromClassPathFile(filename))));//from properties

        BeeObjectSourceConfig config1 = new BeeObjectSourceConfig();
        config1.loadFromPropertiesFile(classFilename);
        Assert.assertTrue(check(config1));

        BeeObjectSourceConfig config2 = new BeeObjectSourceConfig();
        config2.loadFromPropertiesFile(getClassPathFileAbsolutePath(filename));
        Assert.assertTrue(check(config2));

        BeeObjectSourceConfig config3 = new BeeObjectSourceConfig();
        config3.loadFromProperties(loadPropertiesFromClassPathFile(filename));
        Assert.assertTrue(check(config3));
    }


    public void testOnLoadByFileName() throws Exception {
        BeeObjectSourceConfig config1 = new BeeObjectSourceConfig();
        try {
            config1.loadFromPropertiesFile("");
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("file name can't be null or empty"));
        }

        try {
            config1.loadFromPropertiesFile("D:\\beeop\\ob.properties1");
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Configuration file name file must be end with '.properties'"));
        }

        try {
            config1.loadFromPropertiesFile("D:\\beeop\\ob.properties");//file not found
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Not found configuration file"));
        }

        try {//failure test
            String fullFilename = Objects.requireNonNull(getClassPathFileAbsolutePath(filename)).toString();
            String osFileName2 = "beeop/invalid.properties".replace("/", File.separator);
            int lasIndex = fullFilename.lastIndexOf(filename.replace("/", File.separator));
            String invalidFilePath = fullFilename.substring(0, lasIndex) + osFileName2;
            config1.loadFromPropertiesFile(invalidFilePath);//folder test
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Target object is a valid configuration file"));
        }

        //success test: load file from class path
        config1.loadFromPropertiesFile("classpath:" + filename);
        Assert.assertTrue(check(config1));

        //success test: load file from file folder
        config1.loadFromPropertiesFile(Objects.requireNonNull(getClassPathFileAbsolutePath(filename)).getPath());
        Assert.assertTrue(check(config1));
    }

    public void testOnLoadByFile() {
        BeeObjectSourceConfig config1 = new BeeObjectSourceConfig();

        try {//null file test
            config1.loadFromPropertiesFile((File) null);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("can't be null"));
        }

        try {//existence test
            File configFile = new File("c:\\beeop\\ob.properties");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("file not found"));
        }

        try {//existence test
            File configFile = new File("c:\\beeop\\ds");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("file not found"));
        }

        try {//valid file
            String os = System.getProperty("os.name").toLowerCase();
            File configFile = os.contains("windows") ? new File("C:\\") : new File("//");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("is not a valid file"));
        }

        try {//valid file
            Class<?> selfClass = Tc0015ConfigLoadFromFileTest.class;
            URL resource = selfClass.getClassLoader().getResource(filename);
            Assert.assertNotNull(resource);

            String path = resource.getPath();
            String filePath = path.substring(0, path.indexOf("config2.properties")) + "invalid.properties1";
            File configFile = new File(filePath);
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("not a properties file"));
        }
    }

    public void testOnLoadProperties() {
        BeeObjectSourceConfig config1 = new BeeObjectSourceConfig();
        try {
            config1.loadFromProperties(null);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {
            config1.loadFromProperties(new Properties());
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }
    }

    private Boolean check(BeeObjectSourceConfig config) {
        Assert.assertEquals("Pool1", config.getPoolName());
        Assert.assertTrue(config.isFairMode());
        Assert.assertEquals(1, config.getInitialSize());
        Assert.assertEquals(10, config.getMaxActive());
        Assert.assertEquals(8000L, config.getMaxWait());
        Assert.assertEquals(18000L, config.getIdleTimeout());
        Assert.assertEquals(30000L, config.getHoldTimeout());
        Assert.assertEquals(3, config.getAliveTestTimeout());
        Assert.assertEquals(500, config.getAliveAssumeTime());
        Assert.assertEquals(30000, config.getTimerCheckInterval());
        Assert.assertTrue(config.isForceCloseUsingOnClear());
        Assert.assertEquals(3000, config.getParkTimeForRetry());
//        Assert.assertEquals(config.getEvictPredicateClassName(), "com.myProject.TestPredication");
//
//        List<Integer> sqlExceptionCodeList = config.getSqlExceptionCodeList();
//        List<String> sqlExceptionStateList = config.getSqlExceptionStateList();
//        for (Integer code : sqlExceptionCodeList)
//            Assert.assertTrue(code == 500150 || code == 2399);
//        for (String state : sqlExceptionStateList)
//            Assert.assertTrue("0A000".equals(state) || "57P01".equals(state));
//
//        Assert.assertEquals(config.getConnectionFactoryClassName(), "org.stone.beeop.pool.ConnectionFactoryByDriver");
//        Assert.assertEquals(config.getPoolImplementClassName(), "org.stone.beeop.pool.RawConnectionPool");
//        Assert.assertTrue(config.isEnableJmx());

//        Assert.assertEquals(config.getConnectProperty("cachePrepStmts"), "true");
//        Assert.assertEquals(config.getConnectProperty("prepStmtCacheSize"), "50");
//        Assert.assertEquals(config.getConnectProperty("prepStmtCacheSqlLimit"), "2048");
//        Assert.assertEquals(config.getConnectProperty("useServerPrepStmts"), "true");
        return true;
    }
}
