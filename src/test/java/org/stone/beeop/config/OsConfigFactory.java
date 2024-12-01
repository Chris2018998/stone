/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.config;

import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.objects.JavaBookFactory;

/**
 * Config Factory
 *
 * @author Chris Liao
 */

public class OsConfigFactory {

    public static BeeObjectSourceConfig createEmpty() {
        return new BeeObjectSourceConfig();
    }

    public static BeeObjectSourceConfig createDefault() {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactory(new JavaBookFactory());
        return config;
    }
}


