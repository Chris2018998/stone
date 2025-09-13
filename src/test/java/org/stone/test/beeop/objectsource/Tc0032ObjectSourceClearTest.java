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
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.JavaBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0032ObjectSourceClearTest {

    @Test
    public void testClear() throws Exception {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        JavaBookFactory objectFactory = new JavaBookFactory();
        config.setObjectFactory(new JavaBookFactory());
        config.setInitialSize(2);
        BeeObjectSource os = new BeeObjectSource(config);
        Assertions.assertEquals(2, os.getMonitorVo(objectFactory.getDefaultKey()).getIdleSize());

        BeeObjectHandle handle = os.getObjectHandle();
        handle.abort();
        Assertions.assertEquals(1, os.getMonitorVo(objectFactory.getDefaultKey()).getIdleSize());
        os.clear(true);
        Assertions.assertEquals(0, os.getMonitorVo(objectFactory.getDefaultKey()).getIdleSize());

        //2:with new config
        try {
            os.clear(true, null);
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("Configuration can't be null"));
        }
        BeeObjectSourceConfig config2 = new BeeObjectSourceConfig();
        config2.setObjectFactory(new JavaBookFactory());
        config2.setInitialSize(3);
        os.clear(true, config2);
        Assertions.assertEquals(3, os.getMonitorVo(objectFactory.getDefaultKey()).getIdleSize());
    }
}
