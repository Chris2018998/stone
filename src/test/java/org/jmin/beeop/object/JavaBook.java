/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop.object;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class JavaBook implements Book {
    private final String name;
    private final long number;

    public JavaBook() {
        this("Java核心技术·卷2", System.currentTimeMillis());
    }

    public JavaBook(String name, long number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public long getNumber() {
        return number;
    }

    public String toString() {
        return name;
    }
}
