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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceConfigException;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.stone.test.base.TestUtil.getClassPathFileAbsolutePath;
import static org.stone.tools.CommonUtil.loadPropertiesFromClassPathFile;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Tc0011ConfigLoadFromFileTest {
    private static final String filename = "beeop/config2.properties";
    private static final Pattern PATTERN = Pattern.compile("/", Pattern.LITERAL);

    private static boolean check(BeeObjectSourceConfig config) {
        Assertions.assertEquals("Pool1", config.getPoolName());
        Assertions.assertTrue(config.isFairMode());
        Assertions.assertEquals(1, config.getInitialSize());
        Assertions.assertEquals(10, config.getMaxActive());
        Assertions.assertEquals(8000L, config.getMaxWait());
        Assertions.assertEquals(18000L, config.getIdleTimeout());
        Assertions.assertEquals(30000L, config.getHoldTimeout());
        Assertions.assertEquals(3, config.getAliveTestTimeout());
        Assertions.assertEquals(500, config.getAliveAssumeTime());
        Assertions.assertEquals(30000, config.getIntervalOfClearTimeout());
        Assertions.assertTrue(config.isForceRecycleBorrowedOnClose());
        Assertions.assertEquals(3000, config.getParkTimeForRetry());
        return true;
    }

    /********************************************Constructor**************************************************/
    @Test
    public void testOnCorrectFile() throws Exception {
        String classFilename = "cp:" + filename;
        Assertions.assertTrue(check(new BeeObjectSourceConfig(classFilename)));//classpath
        Assertions.assertTrue(check(new BeeObjectSourceConfig(getClassPathFileAbsolutePath(filename))));//from file
        Assertions.assertTrue(check(new BeeObjectSourceConfig(loadPropertiesFromClassPathFile(filename))));//from properties

        BeeObjectSourceConfig config1 = OsConfigFactory.createEmpty();
        config1.loadFromPropertiesFile(classFilename);
        Assertions.assertTrue(check(config1));

        BeeObjectSourceConfig config2 = OsConfigFactory.createEmpty();
        config2.loadFromPropertiesFile(getClassPathFileAbsolutePath(filename));
        Assertions.assertTrue(check(config2));

        BeeObjectSourceConfig config3 = OsConfigFactory.createEmpty();
        config3.loadFromProperties(loadPropertiesFromClassPathFile(filename));
        Assertions.assertTrue(check(config3));
    }

    @Test
    public void testOnLoadByFileName() throws Exception {
        BeeObjectSourceConfig config1 = OsConfigFactory.createEmpty();
        try {
            config1.loadFromPropertiesFile("");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("file name can't be null or empty"));
        }

        try {
            config1.loadFromPropertiesFile("D:\\beeop\\ob.properties1");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration file name file must be end with '.properties'"));
        }

        try {
            config1.loadFromPropertiesFile("D:\\beeop\\ob.properties");//file not found
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Not found configuration file"));
        }

        try {//failure test
            String fullFilename = Objects.requireNonNull(getClassPathFileAbsolutePath(filename)).toString();
            String osFileName2 = PATTERN.matcher("beeop/invalid.properties").replaceAll(Matcher.quoteReplacement(File.separator));
            int lasIndex = fullFilename.lastIndexOf(PATTERN.matcher(filename).replaceAll(Matcher.quoteReplacement(File.separator)));
            String invalidFilePath = fullFilename.substring(0, lasIndex) + osFileName2;
            config1.loadFromPropertiesFile(invalidFilePath);//folder test
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Target object is a valid configuration file"));
        }

        //success test: load file from class path
        config1.loadFromPropertiesFile("classpath:" + filename);
        Assertions.assertTrue(check(config1));

        //success test: load file from file folder
        config1.loadFromPropertiesFile(Objects.requireNonNull(getClassPathFileAbsolutePath(filename)).getPath());
        Assertions.assertTrue(check(config1));
    }

    @Test
    public void testOnLoadByFile() {
        BeeObjectSourceConfig config1 = OsConfigFactory.createEmpty();

        try {//null file test
            config1.loadFromPropertiesFile((File) null);
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("can't be null"));
        }

        try {//existence test
            File configFile = new File("c:\\beeop\\ob.properties");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("file not found"));
        }

        try {//existence test
            File configFile = new File("c:\\beeop\\ds");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("file not found"));
        }

        try {//valid file
            String os = System.getProperty("os.name").toLowerCase();
            File configFile = os.contains("windows") ? new File("C:\\") : new File("//");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("is not a valid file"));
        }

        try {//valid file
            Class<?> selfClass = Tc0011ConfigLoadFromFileTest.class;
            URL resource = selfClass.getClassLoader().getResource(filename);
            Assertions.assertNotNull(resource);

            String path = resource.getPath();
            String filePath = path.substring(0, path.indexOf("config2.properties")) + "invalid.properties1";
            File configFile = new File(filePath);
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("not a properties file"));
        }
    }

    @Test
    public void testOnLoadProperties() {
        BeeObjectSourceConfig config1 = OsConfigFactory.createEmpty();
        try {
            config1.loadFromProperties(null);
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {
            config1.loadFromProperties(new Properties());
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {
            Properties properties = new Properties();
            properties.put("maxActive", "oooo");
            config1.loadFromProperties(properties);
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }
    }

    @Test
    public void testKeyPrefix() {
        try {
            String prefix = "beeop";
            Properties properties = new Properties();
            properties.put("beeop.maxActive", "10");
            BeeObjectSourceConfig config1 = OsConfigFactory.createEmpty();
            config1.loadFromProperties(properties, prefix);
            Assertions.assertEquals(config1.getMaxActive(), 10);
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }

        try {
            String prefix = "beeop.";
            Properties properties = new Properties();
            properties.put("beeop.maxActive", "10");
            properties.put("be.initialSize", "5");
            BeeObjectSourceConfig config2 = OsConfigFactory.createEmpty();
            config2.loadFromProperties(properties, prefix);
            Assertions.assertEquals(config2.getMaxActive(), 10);
            Assertions.assertEquals(config2.getInitialSize(), 0);
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }
    }
}
