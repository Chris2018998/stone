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

/**
 * Configuration items value set/get
 *
 * @author Chris Liao
 */

public class Tc0009ConnectionDefaultValueTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //defaultCatalog
        Assertions.assertNull(config.getDefaultCatalog());//default value check(null)
        config.setDefaultCatalog("catalog");
        Assertions.assertEquals("catalog", config.getDefaultCatalog());
        config.setDefaultCatalog(null);
        Assertions.assertNull(config.getDefaultCatalog());

        //defaultSchema
        Assertions.assertNull(config.getDefaultSchema());//default value check(null)
        config.setDefaultSchema("schema");
        Assertions.assertEquals("schema", config.getDefaultSchema());
        config.setDefaultSchema(null);
        Assertions.assertNull(config.getDefaultSchema());

        //defaultReadOnly
        Assertions.assertNull(config.isDefaultReadOnly());//default value check(null)
        config.setDefaultReadOnly(true);
        Assertions.assertTrue(config.isDefaultReadOnly());
        config.setDefaultReadOnly(false);
        Assertions.assertFalse(config.isDefaultReadOnly());

        //defaultAutoCommit
        Assertions.assertNull(config.isDefaultAutoCommit());//default value check(true)
        config.setDefaultAutoCommit(false);
        Assertions.assertFalse(config.isDefaultAutoCommit());
        config.setDefaultAutoCommit(true);
        Assertions.assertTrue(config.isDefaultAutoCommit());

        //enableDefaultOnCatalog
        Assertions.assertTrue(config.isEnableDefaultOnCatalog());//default value check(true)
        config.setEnableDefaultOnCatalog(false);
        Assertions.assertFalse(config.isEnableDefaultOnCatalog());
        config.setEnableDefaultOnCatalog(true);
        Assertions.assertTrue(config.isEnableDefaultOnCatalog());

        //enableDefaultOnSchema
        Assertions.assertTrue(config.isEnableDefaultOnSchema());//default value check(true)
        config.setEnableDefaultOnSchema(false);
        Assertions.assertFalse(config.isEnableDefaultOnSchema());
        config.setEnableDefaultOnSchema(true);
        Assertions.assertTrue(config.isEnableDefaultOnSchema());

        //enableDefaultOnReadOnly
        Assertions.assertTrue(config.isEnableDefaultOnReadOnly());//default value check(true)
        config.setEnableDefaultOnReadOnly(false);
        Assertions.assertFalse(config.isEnableDefaultOnReadOnly());
        config.setEnableDefaultOnReadOnly(true);
        Assertions.assertTrue(config.isEnableDefaultOnReadOnly());

        //enableDefaultOnReadOnly
        Assertions.assertTrue(config.isEnableDefaultOnAutoCommit());//default check
        config.setEnableDefaultOnAutoCommit(false);
        Assertions.assertFalse(config.isEnableDefaultOnAutoCommit());
        config.setEnableDefaultOnAutoCommit(true);
        Assertions.assertTrue(config.isEnableDefaultOnAutoCommit());

        //enableDefaultOnTransactionIsolation
        Assertions.assertTrue(config.isEnableDefaultOnTransactionIsolation());//default check
        config.setEnableDefaultOnTransactionIsolation(false);
        Assertions.assertFalse(config.isEnableDefaultOnTransactionIsolation());
        config.setEnableDefaultOnTransactionIsolation(true);
        Assertions.assertTrue(config.isEnableDefaultOnTransactionIsolation());

        //forceDirtyOnSchemaAfterSet
        Assertions.assertFalse(config.isForceDirtyOnSchemaAfterSet());//default check
        config.setForceDirtyOnSchemaAfterSet(true);
        Assertions.assertTrue(config.isForceDirtyOnSchemaAfterSet());
        config.setForceDirtyOnSchemaAfterSet(false);
        Assertions.assertFalse(config.isForceDirtyOnSchemaAfterSet());

        //forceDirtyOnCatalogAfterSet
        Assertions.assertFalse(config.isForceDirtyOnCatalogAfterSet());//default check
        config.setForceDirtyOnCatalogAfterSet(true);
        Assertions.assertTrue(config.isForceDirtyOnCatalogAfterSet());
        config.setForceDirtyOnCatalogAfterSet(false);
        Assertions.assertFalse(config.isForceDirtyOnCatalogAfterSet());
    }
}
