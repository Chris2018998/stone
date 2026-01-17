/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.proxy;

import org.stone.beeop.pool.ObjectProxyGenerator;

import java.util.List;

import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * JVM argument；--add-opens java.base/java.lang=ALL-UNNAMED
 *
 * @author Chris Liao
 */
public class ProxyGenerationTest {

    public static void main(String[] ags) throws Exception {
        String interfaceClassName = "org.stone.test.beeop.objects.books.Book";
        String superClassName = "org.stone.test.beeop.objects.books.JavaBook";

        Class<?> superClass = null;
        if (isNotBlank(superClassName)) superClass = Class.forName(superClassName);

        Class<?>[] interfaceClasses = null;
        if (isNotBlank(interfaceClassName)) {
            interfaceClasses = new Class<?>[]{Class.forName(interfaceClassName)};
        }

        List<String> listenNameList = List.of("getName");
        Class[] proxyClasses = ObjectProxyGenerator.genProxyClassWithInterface(superClass, interfaceClasses, listenNameList);

        System.out.println("class0:" + proxyClasses[0].getName());
        System.out.println("class1:" + proxyClasses[1].getName());
    }
}
