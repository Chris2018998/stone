/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beetp.config;

import org.stone.base.TestCase;
import org.stone.beetp.BeeTaskServiceConfig;
import org.stone.beetp.BeeTaskServiceConfigException;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class PoolConfigCheck extends TestCase {

    public void test() throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setInitWorkerSize(2);
        config.setMaxWorkerSize(10);
        config.setTaskMaxSize(50);
        config.setWorkInDaemon(true);
        config.setWorkerKeepAliveTime(6000);

        BeeTaskServiceConfig config2 = config.check();
        if (config2 == config) throw new BeeTaskServiceConfigException("Configuration check copy failed");

        List<String> excludeNames = new LinkedList<String>();
        excludeNames.add("poolName");

        //1:primitive type copy
        Field[] fields = BeeTaskServiceConfig.class.getDeclaredFields();
        for (Field field : fields) {
            if (!excludeNames.contains(field.getName())) {
                field.setAccessible(true);
                if (!Objects.deepEquals(field.get(config), field.get(config2))) {
                    throw new BeeTaskServiceConfigException("Failed to copy field[" + field.getName() + "],value is not equalsString");
                }
            }
        }
    }
}
