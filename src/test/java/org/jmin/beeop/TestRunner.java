/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class TestRunner {
    private static final String defaultFilename = "beeop/testCase.properties";

    @SuppressWarnings("rawtypes")
    private static Class[] getTestCaseClasses() throws Exception {
        return getTestCaseClasses(defaultFilename);
    }

    public static void main(String[] ags) throws Throwable {
        TestRunner.run(getTestCaseClasses());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Class[] getTestCaseClasses(String caseFile) throws Exception {
        List classList = new ArrayList();
        InputStream propertiesStream = null;

        try {
            Properties properties = new SortKeyProperties();
            propertiesStream = TestRunner.class.getResourceAsStream(caseFile);
            propertiesStream = TestRunner.class.getClassLoader().getResourceAsStream(defaultFilename);
            if (propertiesStream == null) propertiesStream = TestRunner.class.getResourceAsStream(defaultFilename);
            if (propertiesStream == null) throw new IOException("Can't find file:'testCase.properties' in classpath");

            String pass1 = "true";
            String pass2 = "Y";
            properties.load(propertiesStream);
            Enumeration<Object> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = properties.getProperty(key);
                if (pass1.equalsIgnoreCase(value) || pass2.equalsIgnoreCase(value)) {
                    Class clazz = Class.forName(key);
                    classList.add(clazz);
                }
            }
            return (Class[]) classList.toArray(new Class[0]);
        } finally {
            if (propertiesStream != null)
                try {
                    propertiesStream.close();
                } catch (IOException e) {
                }
        }
    }

    @SuppressWarnings("rawtypes")
    public static void run(Class testClass) throws Throwable {
        if (testClass != null) {
            ((TestCase) testClass.newInstance()).run();
        }
    }

    @SuppressWarnings("rawtypes")
    public static void run(Class[] testClass) throws Throwable {
        StringBuffer buf = new StringBuffer(50);
        buf.append("*****************************************************************************\n");
        buf.append("*                                                                           *\n");
        buf.append("*                             beeop test begin                              *\n");
        buf.append("*                                                                           *\n");
        buf.append("*                                                     Author:Chris          *\n");
        buf.append("*                                                     All rights reserved   *\n");
        buf.append("*****************************************************************************\n");
        System.out.print(buf);
        if (testClass != null) {
            for (int i = 0; i < testClass.length; i++)
                run(testClass[i]);
        }
    }

    public void testRun() throws Throwable {
        long begtinTime = System.currentTimeMillis();
        TestRunner.run(getTestCaseClasses());
        System.out.println("Took time:(" + (System.currentTimeMillis() - begtinTime) + ")ms");
    }
}

@SuppressWarnings("serial")
class SortKeyProperties extends Properties {
    private final Vector<Object> keyVector = new Vector<Object>();

    public synchronized Enumeration<Object> keys() {
        return keyVector.elements();
    }

    public synchronized Object put(Object key, Object value) {
        Object oldValue = super.put(key, value);
        if (!keyVector.contains(key))
            keyVector.add(key);
        return oldValue;
    }

    public synchronized Object remove(Object key) {
        Object value = super.remove(key);
        keyVector.remove(key);
        return value;
    }
}
