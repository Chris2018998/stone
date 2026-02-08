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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.test.beeop.objects.book.Book;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Tc0013ConfigLoadFromFileTest {
    //classes/beeop/file1.
    private static String beeopResourceAbsolutePath;

    @BeforeAll
    public static void resourcePath() throws Exception {
        String filename = "beeop/config1.properties";//must be existed
        URL resourceURL = Tc0013ConfigLoadFromFileTest.class.getClassLoader().getResource(filename);
        Assertions.assertNotNull(resourceURL);
        File resourceFile = new File(resourceURL.toURI());
        beeopResourceAbsolutePath = resourceFile.getParent();
    }

    private static boolean check(BeeObjectSourceConfig<String, Book> config) {
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
        String filename = "config1.properties";
        String classPathFilename1 = "cp:beeop/" + filename;
        String classPathFilename2 = "classpath:beeop/" + filename;
        String absolutePathFileName = beeopResourceAbsolutePath + File.separator + filename;
        File fileFile = new File(absolutePathFileName);

        //1: load file in constructor
        //1.1: load file from class path
        Assertions.assertTrue(check(new BeeObjectSourceConfig<>(classPathFilename1)));//classpath
        Assertions.assertTrue(check(new BeeObjectSourceConfig<>(classPathFilename2)));//classpath

        //1.2: load file from absolution path
        Assertions.assertTrue(check(new BeeObjectSourceConfig<>(fileFile)));//from file
        Assertions.assertTrue(check(new BeeObjectSourceConfig<>(absolutePathFileName)));//from file

        //1.3: load from Properties
        Properties properties = new Properties();
        try (FileInputStream fileStream = new FileInputStream(fileFile)) {
            properties.load(fileStream);
        }
        Assertions.assertTrue(check(new BeeObjectSourceConfig<>(properties)));//from properties

        //2: load configuration by methods
        //2.1: load file from class path
        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createEmpty();
        config1.loadFromPropertiesFile(classPathFilename1);
        Assertions.assertTrue(check(config1));
        config1 = OsConfigFactory.createEmpty();
        config1.loadFromPropertiesFile(classPathFilename1);
        Assertions.assertTrue(check(config1));
        //2.2: load file from absolution path
        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createEmpty();
        config2.loadFromPropertiesFile(fileFile);
        Assertions.assertTrue(check(config2));
        config2 = OsConfigFactory.createEmpty();
        config2.loadFromPropertiesFile(absolutePathFileName);
        Assertions.assertTrue(check(config2));

        //2.3: load file from absolution path
        BeeObjectSourceConfig<String, Book> config3 = OsConfigFactory.createEmpty();
        config3.loadFromProperties(properties);
        Assertions.assertTrue(check(config3));
    }

    @Test
    public void testInvalidFileName() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        try {//null filename
            config.loadFromPropertiesFile((String) null);
            fail("[testInvalidFileName]failed");
        } catch (Exception e) {
            Assertions.assertEquals("Load file name cannot be null or empty", e.getMessage());
        }

        try {//blank filename
            config.loadFromPropertiesFile("");
            fail("[testInvalidFileName]failed");
        } catch (Exception e) {
            Assertions.assertEquals("Load file name cannot be null or empty", e.getMessage());
        }

        try {//file extension name test
            config.loadFromPropertiesFile(beeopResourceAbsolutePath + File.separator + "invalidProperties");
            fail("[testInvalidFileName]failed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load file extension name must be 'properties':"));
        }
    }

    @Test
    public void testInvalidFile() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        try {//null filename
            config.loadFromPropertiesFile((File) null);
            fail("[testInvalidFile]failed");
        } catch (Exception e) {
            Assertions.assertEquals("Load file cannot be null", e.getMessage());
        }

        try {//file not found test
            config.loadFromPropertiesFile(new File(beeopResourceAbsolutePath + File.separator + "not_found.properties"));
            fail("[testInvalidFile]failed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load file not found"));
        }

        try {//test file is a folder
            config.loadFromPropertiesFile(new File(beeopResourceAbsolutePath + File.separator + "empty"));
            fail("[testInvalidFile]failed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load file cannot be a folder"), message);
        }

        try {//file extension name test
            config.loadFromPropertiesFile(new File(beeopResourceAbsolutePath + File.separator + "invalidProperties"));
            fail("[testInvalidFile]failed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load file extension name must be 'properties':"), message);
        }
    }

    @Test
    public void testLoadProperties() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        try {
            config.loadFromProperties(null);
            fail("[testLoadProperties]not threw exception when loading null properties file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load properties cannot be null or empty"));
        }

        try {//correct
            config.loadFromProperties(new Properties());
            fail("[testLoadProperties]not threw exception when loading empty properties");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load properties cannot be null or empty"));
        }

        try {//correct
            Properties properties = new Properties();
            properties.put("maxActive", "oooo");
            config.loadFromProperties(properties);
            fail("[testLoadProperties]not threw exception when loading invalid properties item");
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }
    }
}
