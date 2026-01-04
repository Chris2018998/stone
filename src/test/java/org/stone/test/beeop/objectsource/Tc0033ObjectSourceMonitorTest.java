/// *
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// *
// * Copyright(C) Chris2018998,All rights reserved.
// *
// * Project owner contact:Chris2018998@tom.com.
// *
// * Project Licensed under Apache License v2.0
// */
//package org.stone.test.beeop.objectsource;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.stone.beeop.BeeObjectSource;
//import org.stone.beeop.BeeObjectSourceConfig;
//import org.stone.test.beeop.objects.JavaBookFactory;
//
/// **
// * @author Chris Liao
// */
//public class Tc0033ObjectSourceMonitorTest {
//
//    @Test
//    public void testGetMonitor() throws Exception {
//        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
//        JavaBookFactory objectFactory = new JavaBookFactory();
//        config.setObjectFactory(new JavaBookFactory());
//        config.setInitialSize(2);
//        BeeObjectSource os = new BeeObjectSource(config);
//
//        Assertions.assertEquals(2, os.getPoolMonitorVo().getIdleSize());
//        Assertions.assertEquals(2, os.getMonitorVo(objectFactory.getDefaultKey()).getIdleSize());
//    }
//}
