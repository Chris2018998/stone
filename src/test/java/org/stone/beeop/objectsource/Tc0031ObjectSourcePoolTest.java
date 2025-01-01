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
import org.stone.base.StoneLogAppender;
import org.stone.base.TestUtil;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;

import static org.stone.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0031ObjectSourcePoolTest extends TestCase {

    public void testOnInitializedPool() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setPrintConfigInfo(false);
        BeeObjectSource ds = null;

        try {
            ds = new BeeObjectSource(config);
            StoneLogAppender logAppender = TestUtil.getStoneLogAppender();
            logAppender.beginCollectStoneLog();

            //getConnection test
            BeeObjectHandle handle1 = ds.getObjectHandle();
            Assert.assertNotNull(handle1);
            handle1.close();
        } finally {
            if (ds != null && !ds.isClosed()) {
                ds.close();
                Assert.assertTrue(ds.isClosed());
            }
        }
    }


//    public void testOnUninitializedPool() throws Exception {
//        BeeObjectSource ds = new BeeObjectSource();
//        BeeConnectionPool pool = (BeeConnectionPool) TestUtil.getFieldValue(ds, "pool");
//        Assert.assertNull(pool);
//        Assert.assertTrue(ds.isClosed());
//
//        //test on methods of commonDataSource
//        Assert.assertNull(ds.getParentLogger());
//        Assert.assertNull(ds.getLogWriter());
//        ds.setLogWriter(new PrintWriter(System.out));
//        Assert.assertNull(ds.getLogWriter());
//        Assert.assertEquals(0, ds.getLoginTimeout());
//        Assert.assertEquals(0, DriverManager.getLoginTimeout());
//        ds.setLoginTimeout(10);//ten seconds
//        Assert.assertEquals(0, ds.getLoginTimeout());
//        Assert.assertEquals(0, DriverManager.getLoginTimeout());
//
//        ds.setPrintRuntimeLog(true);
//        try {
//            ds.getPoolMonitorVo();
//        } catch (PoolNotCreatedException e) {
//            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
//        }
//        try {
//            ds.getConnectionCreatingCount();
//        } catch (PoolNotCreatedException e) {
//            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
//        }
//
//        try {
//            ds.getConnectionCreatingTimeoutCount();
//        } catch (PoolNotCreatedException e) {
//            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
//        }
//
//        try {
//            ds.interruptConnectionCreating(false);
//        } catch (PoolNotCreatedException e) {
//            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
//        }
//
//        try {
//            ds.clear(true);
//        } catch (PoolNotCreatedException e) {
//            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
//        }
//
//        try {
//            ds.clear(true, null);
//        } catch (PoolNotCreatedException e) {
//            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
//        }
//
//        BeeObjectSourceConfig config = createDefault();
//        MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
//        factory.setReturnNullOnCreate(true);
//        config.setConnectionFactory(factory);
//        new BeeObjectSource(config);
//    }
//
//    public void testPoolClassNotFound() {
//        BeeObjectSource ds = null;
//        Connection con = null;
//        try {//lazy creation
//            ds = new BeeObjectSource(JDBC_DRIVER, JDBC_URL, JDBC_USER, JDBC_PASSWORD);
//            ds.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
//            con = ds.getConnection();
//        } catch (SQLException e) {
//            Throwable poolCause = e.getCause();
//            Assert.assertTrue(poolCause instanceof ClassNotFoundException);
//        } finally {
//            if (con != null) ConnectionPoolStatics.oclose(con);
//            if (ds != null) ds.close();
//        }
//
//        BeeObjectSource ds2 = null;
//        try {//creation in constructor
//            BeeObjectSourceConfig config = createDefault();
//            config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
//            ds2 = new BeeObjectSource(config);
//        } catch (RuntimeException e) {
//            Throwable cause = e.getCause();
//            Assert.assertTrue(cause instanceof PoolCreateFailedException);
//            PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
//            Throwable poolCause = poolException.getCause();
//            Assert.assertTrue(poolCause instanceof ClassNotFoundException);
//        } finally {
//            if (ds2 != null) ds2.close();
//        }
//    }
//
//    public void testPoolInitializeFailedException() {
//        BeeObjectSource ds = null;
//        try {
//            BeeObjectSourceConfig config = createDefault();
//            config.setMaxActive(5);
//            config.setInitialSize(10);
//            ds = new BeeObjectSource(config);
//        } catch (RuntimeException e) {
//            Throwable cause = e.getCause();
//            Assert.assertTrue(cause instanceof PoolInitializeFailedException);
//            PoolInitializeFailedException poolInitializeException = (PoolInitializeFailedException) cause;
//            Assert.assertTrue(poolInitializeException.getCause() instanceof BeeObjectSourceConfigException);
//            Throwable bottomException = poolInitializeException.getCause();
//            Assert.assertTrue(bottomException.getMessage().contains("initialSize must not be greater than maxActive"));
//        } finally {
//            if (ds != null) ds.close();
//        }
//    }
}
