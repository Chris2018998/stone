/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop.config;

import org.jmin.beeop.BeeObjectSourceConfig;
import org.jmin.beeop.BeeObjectSourceConfigException;
import org.jmin.beeop.TestCase;
import org.jmin.beeop.TestUtil;
import org.jmin.beeop.object.JavaBook;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ConfigCheckCopyTest extends TestCase {
    public void test() throws Exception {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectClassName(JavaBook.class.getName());
        BeeObjectSourceConfig config2 = config.check();

        if (config2 == config) throw new BeeObjectSourceConfigException("Configuration check copy failed");

        List<String> excludeNames = new LinkedList<String>();
        excludeNames.add("objectFactory");
        excludeNames.add("objectInterfaces");
        excludeNames.add("objectInterfaceNames");
        excludeNames.add("excludeMethodNames");

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
        Set<String> excludeMethodNames1 = (Set<String>) TestUtil.getFieldValue(config, "excludeMethodNames");
        Set<String> excludeMethodNames2 = (Set<String>) TestUtil.getFieldValue(config2, "excludeMethodNames");
        if (!Objects.deepEquals(excludeMethodNames1, excludeMethodNames2))
            throw new BeeObjectSourceConfigException("Configuration 'excludeMethodNames' check copy failed");
        if (!Objects.deepEquals(config.getObjectInterfaceNames(), config2.getObjectInterfaceNames()))
            throw new BeeObjectSourceConfigException("Configuration 'objectInterfaceNames' check copy failed");
    }
}