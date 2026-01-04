/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.study.mbean;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * Test Class
 *
 * @author chris liao
 */

public class HelloServer {

    public static void main(String[] args) throws InterruptedException, MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.stone.test.study.mbean:type=Hello");
        Hello mbean = new Hello();
        mbs.registerMBean(mbean, name);
        Thread.sleep(Long.MAX_VALUE);
    }
}
