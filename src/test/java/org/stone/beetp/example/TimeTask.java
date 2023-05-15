/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.example;

import org.stone.beetp.BeeTask;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Time schedule demo
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TimeTask implements BeeTask {
    private SimpleDateFormat format;

    public TimeTask() {
        this.format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    }

    public Object call() {
        String time = "Current time:" + format.format(new Date());
        System.out.println(time);
        return time;
    }
}
