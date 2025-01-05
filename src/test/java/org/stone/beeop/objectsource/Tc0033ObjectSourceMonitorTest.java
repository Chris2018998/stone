/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.objectsource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.objects.JavaBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0033ObjectSourceMonitorTest extends TestCase {

    public void testGetMonitor() throws Exception {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        JavaBookFactory objectFactory = new JavaBookFactory();
        config.setObjectFactory(new JavaBookFactory());
        config.setInitialSize(2);
        BeeObjectSource os = new BeeObjectSource(config);

        Assert.assertEquals(2, os.getPoolMonitorVo().getIdleSize());
        Assert.assertEquals(2, os.getMonitorVo(objectFactory.getDefaultKey()).getIdleSize());
    }
}
