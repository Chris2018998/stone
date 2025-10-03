/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceCreationException;
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * DataSource Creation test
 *
 * @author Chris Liao
 */

public class Tc0030DsCreationTest {

    @Test
    public void testCreation() {//success test
        //1: constructor without parameters
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertNotNull(ds);
        } catch (Exception e) {
            fail("[testCreation]threw exception when create datasource object by default constructor");
        }

        //2: constructor with 4 jdbc link parameters
        try (BeeDataSource ds = new BeeDataSource(JDBC_DRIVER, JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            Assertions.assertNotNull(ds);
        } catch (Exception e) {
            fail("[testCreation]threw exception when create datasource with configuration object");
        }

        //3: constructor with a dataSourceConfig object
        try (BeeDataSource ds = new BeeDataSource(createDefault())) {
            Assertions.assertNotNull(ds);
        } catch (Exception e) {
            fail("[testCreation]threw exception when create datasource with configuration object");
        }

        //4: constructor with DataSourceConfig object(no connection factory,no jdbc link info)
        try (BeeDataSource ds = new BeeDataSource(new BeeDataSourceConfig())) {
            Assertions.assertNotNull(ds);
        } catch (BeeDataSourceCreationException e) {
            Throwable cause = e.getCause();
            Assertions.assertInstanceOf(PoolInitializeFailedException.class, cause);
            Throwable poolFailedCause = cause.getCause();
            Assertions.assertEquals("jdbcUrl must not be null or blank", poolFailedCause.getMessage());
        }
    }

    @Test
    public void testInvalidPoolClass() {//fail test
        BeeDataSourceConfig config = createDefault();
        config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name

        try (BeeDataSource ignored = new BeeDataSource(config)) {
            fail("[testDataSourceCreateFailed]not threw exception when Data source created with an invalid pool class");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assertions.assertInstanceOf(PoolCreateFailedException.class, cause);

            PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
            Throwable poolCause = poolException.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, poolCause);
        }
    }
}
