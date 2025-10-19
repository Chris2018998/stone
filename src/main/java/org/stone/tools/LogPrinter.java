/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A logger wrapper on SLF logger for stone project.
 *
 * @author Chris Liao
 */
public final class LogPrinter {
    //a SLF4 logger used in stone project
    public static final LogPrinter CommonLogPrinter = LogPrinter.getLogPrinter(BeanUtil.class, true);

    private final Logger logger;

    private boolean outputLogs;

    private LogPrinter(Logger logger, boolean outputLogs) {
        this.logger = logger;
        this.outputLogs = outputLogs;
    }

    public static LogPrinter getLogPrinter(String className, boolean outputLogs) {
        return new LogPrinter(LoggerFactory.getLogger(className), outputLogs);
    }

    public static LogPrinter getLogPrinter(Class<?> clazz, boolean outputLogs) {
        return new LogPrinter(LoggerFactory.getLogger(clazz), outputLogs);
    }

    public boolean isOutputLogs() {
        return outputLogs;
    }

    public void setOutputLogs(boolean outputLogs) {
        this.outputLogs = outputLogs;
    }

    //****************************************************************************************************************//
    //                                     1: debug                                                                   //
    //****************************************************************************************************************//
    public void debug(String s) {
        if (outputLogs) logger.debug(s);
    }

    public void debug(String s, Object... var2) {
        if (outputLogs) logger.debug(s, var2);
    }

    public void debug(String s, Throwable e) {
        if (outputLogs) logger.debug(s, e);
    }

    public void debug(String s, Throwable e, Object... var2) {
        if (outputLogs) logger.debug(s, e, var2);
    }

    //****************************************************************************************************************//
    //                                     2: info                                                                    //
    //****************************************************************************************************************//
    public void info(String s) {
        if (outputLogs) logger.info(s);
    }

    public void info(String s, Object... var2) {
        if (outputLogs) logger.info(s, var2);
    }

    public void info(String s, Throwable e) {
        if (outputLogs) logger.info(s, e);
    }

    public void info(String s, Throwable e, Object... var2) {
        if (outputLogs) logger.info(s, e, var2);
    }

    //****************************************************************************************************************//
    //                                     3: warn                                                                    //
    //****************************************************************************************************************//
    public void warn(String s) {
        if (outputLogs) logger.warn(s);
    }

    public void warn(String s, Object... var2) {
        if (outputLogs) logger.warn(s, var2);
    }

    public void warn(String s, Throwable e) {
        if (outputLogs) logger.warn(s, e);
    }

    public void warn(String s, Throwable e, Object... var2) {
        if (outputLogs) logger.warn(s, e, var2);
    }

    //****************************************************************************************************************//
    //                                     4: error                                                                   //
    //****************************************************************************************************************//
    public void error(String s) {
        if (outputLogs) logger.error(s);
    }

    public void error(String s, Object... var2) {
        if (outputLogs) logger.error(s, var2);
    }

    public void error(String s, Throwable e) {
        if (outputLogs) logger.error(s, e);
    }

    public void error(String s, Throwable e, Object... var2) {
        if (outputLogs) logger.error(s, e, var2);
    }
}
