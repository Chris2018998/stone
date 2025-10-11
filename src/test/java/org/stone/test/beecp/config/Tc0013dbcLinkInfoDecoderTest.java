/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeJdbcLinkInfoDecoder;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0013dbcLinkInfoDecoderTest {
    private static final String username = "user";
    private static final String password = "password";
    private static final String url = MOCK_URL;
    private static final String driver = MOCK_DRIVER;

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Class<? extends BeeJdbcLinkInfoDecoder> decodeClass = SampleMockJdbcLinkInfoDecoder.class;
        config.setLinkInfoDecoderClass(decodeClass);
        Assertions.assertEquals(decodeClass, config.getLinkInfoDecoderClass());

        String decodeClassName = "org.stone.beecp.objects.SampleJdbcLinkInfoDecoder";
        config.setLinkInfoDecoderClassName(decodeClassName);
        Assertions.assertEquals(decodeClassName, config.getLinkInfoDecoderClassName());

        SampleMockJdbcLinkInfoDecoder decoder = new SampleMockJdbcLinkInfoDecoder();
        config.setLinkInfoDecoder(decoder);
        Assertions.assertEquals(config.getLinkInfoDecoder(), decoder);
    }

    @Test
    public void testOnCreation() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, url, username, password);
        config1.setLinkInfoDecoder(new SampleMockJdbcLinkInfoDecoder());
        config1.setConnectionFactoryClass(MockConnectionFactory.class);
        BeeDataSourceConfig checkConfig = config1.check();

        MockConnectionFactory factory = (MockConnectionFactory) checkConfig.getConnectionFactory();
        String url = factory.getJdbcUrl();
        String user = factory.getUsername();
        String password = factory.getPassword();
        Assertions.assertTrue(url.endsWith("-Decoded"));
        Assertions.assertTrue(user.endsWith("-Decoded"));
        Assertions.assertTrue(password.endsWith("-Decoded"));

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setConnectionFactoryClass(MockConnectionFactory.class);
        checkConfig = config2.check();
        factory = (MockConnectionFactory) checkConfig.getConnectionFactory();

        String decodedUrl = factory.getJdbcUrl();
        String decodedUser = factory.getUsername();
        String decodedPassword = factory.getPassword();
        Assertions.assertEquals(url, decodedUrl);
        Assertions.assertEquals(username, decodedUser);
        Assertions.assertEquals(password, decodedPassword);

        BeeDataSourceConfig config3 = new BeeDataSourceConfig(driver, url, username, password);
        config3.setConnectionFactoryClass(MockConnectionFactory.class);
        config3.setLinkInfoDecoderClassName("org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        checkConfig = config3.check();
        factory = (MockConnectionFactory) checkConfig.getConnectionFactory();
        url = factory.getJdbcUrl();
        user = factory.getUsername();
        password = factory.getPassword();
        Assertions.assertTrue(url.endsWith("-Decoded"));
        Assertions.assertTrue(user.endsWith("-Decoded"));
        Assertions.assertTrue(password.endsWith("-Decoded"));

        BeeDataSourceConfig config4 = new BeeDataSourceConfig(driver, url, username, password);
        config4.setConnectionFactoryClass(MockConnectionFactory.class);
        config4.setLinkInfoDecoderClassName("org.stone.beecp.BeeJdbcLinkInfoDecoder");
        checkConfig = config4.check();
        factory = (MockConnectionFactory) checkConfig.getConnectionFactory();
        String factoryUrl = factory.getJdbcUrl();
        String factoryUser = factory.getUsername();
        String factoryPassword = factory.getPassword();
        Assertions.assertEquals(url, factoryUrl);
        Assertions.assertEquals(username, factoryUser);
        Assertions.assertEquals(password, factoryPassword);
    }

    @Test
    public void testOnErrorClass() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);
        config.setLinkInfoDecoderClassName("java.lang.String");//error config
        try {
            config.check();
            fail("[testOnErrorClass]not threw exception when check invalid className of Jdbc-linkInfo-decoder");
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            String message = cause.getMessage();
            Assertions.assertTrue(message != null && message.contains("decoder"));
        }
    }

    @Test
    public void testOnErrorClassName() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);

        config.setLinkInfoDecoderClassName("String");//error config
        try {
            config.check();
            fail("[testOnErrorClassName]not threw exception when check invalid className of Jdbc-linkInfo-decoder");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("decoder"));
        }
    }

    /****************************************************Decode execution Test ****************************************/
    @Test
    public void testJdbcDecoderOnDriver() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, url, null, null);
        config1.setLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        Object factory1 = TestUtil.getFieldValue(checkedConfig1, "connectionFactory");
        Properties properties1 = (Properties) TestUtil.getFieldValue(factory1, "properties");
        Assertions.assertFalse(properties1.contains("user"));
        Assertions.assertFalse(properties1.contains("password"));

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        Object factory2 = TestUtil.getFieldValue(checkedConfig2, "connectionFactory");
        String url2 = (String) TestUtil.getFieldValue(factory2, "url");
        Properties properties = (Properties) TestUtil.getFieldValue(factory2, "properties");
        String user2 = properties.getProperty("user");
        String password2 = properties.getProperty("password");
        Assertions.assertTrue(url2.endsWith("-Decoded"));
        Assertions.assertTrue(user2.endsWith("-Decoded"));
        Assertions.assertTrue(password2.endsWith("-Decoded"));

        BeeDataSourceConfig config3 = new BeeDataSourceConfig(driver, url, username, null);
        config3.setLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig3 = config3.check();
        Object factory3 = TestUtil.getFieldValue(checkedConfig3, "connectionFactory");
        String url3 = (String) TestUtil.getFieldValue(factory3, "url");
        Properties properties3 = (Properties) TestUtil.getFieldValue(factory3, "properties");
        String user3 = properties3.getProperty("user");
        String password3 = properties3.getProperty("password");

        Assertions.assertTrue(url3.endsWith("-Decoded"));
        Assertions.assertTrue(user3.endsWith("-Decoded"));
        Assertions.assertNull(password3);
    }

    @Test
    public void testJdbcDecoderOnFactory() throws Exception {
        clearBeeCPInfoFromSystemProperties();
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactoryClass(MockConnectionFactory.class);
        config1.setLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        MockConnectionFactory factory1 = (MockConnectionFactory) checkedConfig1.getConnectionFactory();
        Assertions.assertNull(factory1.getUsername());
        Assertions.assertNull(factory1.getPassword());

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setUsername(username);
        config2.setConnectionFactoryClass(MockConnectionFactory.class);
        config2.setLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        MockConnectionFactory factory2 = (MockConnectionFactory) checkedConfig2.getConnectionFactory();
        Assertions.assertTrue(factory2.getUsername().endsWith("-Decoded"));
        Assertions.assertNull(factory2.getPassword());

        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setUrl(url);
        config3.setUsername(username);
        config3.setPassword(password);
        config3.setConnectionFactoryClass(MockConnectionFactory.class);
        config3.setLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig3 = config3.check();
        MockConnectionFactory factory3 = (MockConnectionFactory) checkedConfig3.getConnectionFactory();
        Assertions.assertTrue(factory3.getJdbcUrl().endsWith("-Decoded"));
        Assertions.assertTrue(factory3.getUsername().endsWith("-Decoded"));
        Assertions.assertTrue(factory3.getPassword().endsWith("-Decoded"));

        BeeDataSourceConfig config4 = new BeeDataSourceConfig();
        config4.addConnectProperty("url", url);
        config4.addConnectProperty("user", username);
        config4.addConnectProperty("password", password);
        config4.setConnectionFactoryClass(MockConnectionFactory.class);
        config4.setLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig4 = config4.check();
        MockConnectionFactory factory4 = (MockConnectionFactory) checkedConfig4.getConnectionFactory();
        Assertions.assertTrue(factory4.getJdbcUrl().endsWith("-Decoded"));
        Assertions.assertTrue(factory4.getUsername().endsWith("-Decoded"));
        Assertions.assertTrue(factory4.getPassword().endsWith("-Decoded"));

        try {
            BeeDataSourceConfig config5 = new BeeDataSourceConfig();
            clearBeeCPInfoFromSystemProperties();
            System.setProperty("beecp.url", url);
            System.setProperty("beecp.user", username);
            System.setProperty("beecp.password", password);
            config5.setConnectionFactoryClass(MockConnectionFactory.class);
            config5.setLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
            BeeDataSourceConfig checkedConfig5 = config5.check();
            MockConnectionFactory factory5 = (MockConnectionFactory) checkedConfig5.getConnectionFactory();
            Assertions.assertTrue(factory5.getJdbcUrl().endsWith("-Decoded"));
            Assertions.assertTrue(factory5.getUsername().endsWith("-Decoded"));
            Assertions.assertTrue(factory5.getPassword().endsWith("-Decoded"));
        } finally {
            System.clearProperty("beecp.url");
            System.clearProperty("beecp.user");
            System.clearProperty("beecp.password");
        }
    }
}
