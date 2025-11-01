/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.test.beetp.config;

import org.stone.beetp.TaskServiceConfig;
import org.stone.beetp.TaskServiceConfigException;
import org.stone.test.base.TestCase;

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
        TaskServiceConfig config = new TaskServiceConfig();
        config.setMaxOnceTaskSize(50);
        config.setOnceWorkerKeepAliveTime(6000);

        TaskServiceConfig config2 = config.check();
        if (config2 == config) throw new TaskServiceConfigException("Configuration check copy failed");

        List<String> excludeNames = new LinkedList<String>();
        excludeNames.add("poolName");

        //1:primitive type copy
        Field[] fields = TaskServiceConfig.class.getDeclaredFields();
        for (Field field : fields) {
            if (!excludeNames.contains(field.getName())) {
                field.setAccessible(true);
                if (!Objects.deepEquals(field.get(config), field.get(config2))) {
                    throw new TaskServiceConfigException("Failed to copy field[" + field.getName() + "],value is not equalsString");
                }
            }
        }
    }
}
