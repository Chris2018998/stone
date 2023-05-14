/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.config;

import org.stone.base.TestCase;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.BeeObjectSourceConfigException;
import org.stone.beeop.object.JavaBookFactory;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ConfigCheckCopyTest extends TestCase {
    public void test() throws Exception {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactoryClassName(JavaBookFactory.class.getName());
        BeeObjectSourceConfig config2 = config.check();

        if (config2 == config) throw new BeeObjectSourceConfigException("Configuration check copy failed");

        List<String> excludeNames = new LinkedList<String>();
        excludeNames.add("poolName");
        excludeNames.add("objectFactory");
        excludeNames.add("objectInterfaces");
        excludeNames.add("objectInterfaceNames");

        //1:primitive type copy
        Field[] fields = BeeObjectSourceConfig.class.getDeclaredFields();
        for (Field field : fields) {
            if (!excludeNames.contains(field.getName())) {
                field.setAccessible(true);
                if (!Objects.deepEquals(field.get(config), field.get(config2))) {
                    throw new BeeObjectSourceConfigException("Failed to copy field[" + field.getName() + "],value is not equalsString");
                }
            }
        }

        //2:test container type properties
        if (!Objects.deepEquals(config.getObjectInterfaceNames(), config2.getObjectInterfaceNames()))
            throw new BeeObjectSourceConfigException("Configuration 'objectInterfaceNames' check copy failed");
    }
}