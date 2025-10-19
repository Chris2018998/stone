/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects;

import org.stone.beeop.BeeKeyedObjectPool;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSourceConfig;

import java.util.List;

public class MockBlockPoolImplementation implements BeeKeyedObjectPool {
    public void start(BeeObjectSourceConfig config) throws Exception {
    }

    public void close() {
    }

    public boolean isClosed() {
        return false;
    }

    public boolean isReady() {
        return false;
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    public void restart(boolean forceCloseUsing) throws Exception {
    }

    public void restart(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {
    }

    public BeeObjectHandle getObjectHandle() {
        return new MockObjectHandleImpl();
    }

    public BeeObjectHandle getObjectHandle(Object key) {
        return new MockObjectHandleImpl();
    }

    public List<Thread> interruptWaitingThreads() {
        return null;
    }

    public List<Thread> interruptWaitingThreads(Object key) {
        return null;
    }

    public void enableLogPrint(boolean indicator) {
    }

    public boolean isEnabledLogPrint(Object key) {
        return false;
    }

    public void enableLogPrint(Object key, boolean indicator) throws Exception {
    }

    public BeeObjectPoolMonitorVo getMonitorVo(Object key) {
        return null;
    }

    public int keySize() {
        return 1;
    }

    public Object[] keys() {
        return null;
    }

    public boolean exists(Object key) {
        return true;
    }

    public void reset(Object key) {
    }

    public void reset(Object key, boolean forceCloseUsing) {
    }

    public void deleteKey(Object key) {
    }

    public void deleteKey(Object key, boolean forceCloseUsing) {
    }

    /**
     * Queries logs collector in whether in being enabled.
     *
     * @return boolean true is enabled,false is disabled
     */
    public boolean isEnabledObjectCallLogManager() {
        return false;
    }

    /**
     * A switch to enable or disable configured log collector in pool.
     *
     * @param enable is true that enable, false is disabled
     */
    public void enableObjectCallLogManager(boolean enable) {

    }
}
