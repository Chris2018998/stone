/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp;

import org.stone.beetp.pool.exception.TaskException;
import org.stone.beetp.pool.exception.TaskPoolException;
import org.stone.beetp.pool.exception.TaskServiceConfigException;

import java.util.concurrent.TimeUnit;

/**
 * Task pool interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskPool {

    void init(TaskServiceConfig config) throws TaskPoolException, TaskServiceConfigException;


    <V> TaskHandle<V> submit(Task<V> task) throws TaskException;

    <V> TaskHandle<V> submit(Task<V> task, TaskAspect<V> aspect) throws TaskException;

    <V> TaskHandle<V> submit(Task<V> task, TaskJoinOperator<V> joinOperator) throws TaskException;

    <V> TaskHandle<V> submit(Task<V> task, TaskJoinOperator<V> joinOperator, TaskAspect<V> aspect) throws TaskException;

    <V> TaskHandle<V> submit(TreeLayerTask<V> task) throws TaskException;

    <V> TaskHandle<V> submit(TreeLayerTask<V> task, TaskAspect<V> aspect) throws TaskException;


    <V> TaskScheduledHandle<V> schedule(Task<V> task, long delay, TimeUnit unit) throws TaskException;

    <V> TaskScheduledHandle<V> schedule(Task<V> task, long delay, TimeUnit unit, TaskAspect<V> aspect) throws TaskException;

    <V> TaskScheduledHandle<V> scheduleAtFixedRate(Task<V> task, long initialDelay, long period, TimeUnit unit) throws TaskException;

    <V> TaskScheduledHandle<V> scheduleAtFixedRate(Task<V> task, long initialDelay, long period, TimeUnit unit, TaskAspect<V> aspect) throws TaskException;

    <V> TaskScheduledHandle<V> scheduleWithFixedDelay(Task<V> task, long initialDelay, long delay, TimeUnit unit) throws TaskException;

    <V> TaskScheduledHandle<V> scheduleWithFixedDelay(Task<V> task, long initialDelay, long delay, TimeUnit unit, TaskAspect<V> aspect) throws TaskException;


    boolean isTerminated();

    boolean isTerminating();

    int getRunningCount();

    long getCompletedCount();

    TaskPoolMonitorVo getPoolMonitorVo();


    boolean clear(boolean mayInterruptIfRunning) throws TaskServiceConfigException;

    boolean clear(boolean mayInterruptIfRunning, TaskServiceConfig config) throws TaskServiceConfigException;


    TaskPoolTerminatedVo terminate(boolean mayInterruptIfRunning) throws TaskPoolException;

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

}
