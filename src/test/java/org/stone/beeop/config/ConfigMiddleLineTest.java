/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.config;

import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.BeeObjectSourceConfigException;
import org.stone.beeop.TestCase;

import java.net.URL;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ConfigMiddleLineTest extends TestCase {
    public void test() throws Exception {
        String filename = "beeop/ConfigMiddleLineTest.properties";
        URL url = ConfigMiddleLineTest.class.getResource(filename);
        url = ConfigMiddleLineTest.class.getClassLoader().getResource(filename);

        BeeObjectSourceConfig testConfig = new BeeObjectSourceConfig();
        testConfig.loadFromPropertiesFile(url.getFile());

        if (!"BeeOP".equals(testConfig.getPoolName())) throw new BeeObjectSourceConfigException("poolName error");
        if (!testConfig.isFairMode()) throw new BeeObjectSourceConfigException("fairMode error");
        if (testConfig.getInitialSize() != 5) throw new BeeObjectSourceConfigException("initialSize error");
        if (testConfig.getMaxActive() != 10) throw new BeeObjectSourceConfigException("maxActive error");
    }
}