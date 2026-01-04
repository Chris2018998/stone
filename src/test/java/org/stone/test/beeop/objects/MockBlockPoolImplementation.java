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

import org.stone.beeop.*;

import java.util.List;

public class MockBlockPoolImplementation<K, V> implements BeeKeyedObjectPool<K, V> {

    public void start(BeeObjectSourceConfig<K, V> config) throws Exception {

    }

    public void restart(boolean forceRecycleBorrowed) throws Exception {

    }

    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {

    }

    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        return null;
    }


    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        return null;
    }


    public boolean suspendKey(K key) throws Exception {
        return false;
    }


    public boolean resumeKey(K key) throws Exception {
        return false;
    }

    public void close() {
    }

    public boolean suspendPool() {
        return false;
    }

    public boolean resumePool() {
        return false;
    }

    public boolean isClosed() {
        return false;
    }

    public boolean isReady() {
        return false;
    }

    public void enableLogPrint(boolean enable) {

    }

    public boolean isEnabledLogPrint() {
        return false;
    }

    public List<Thread> interruptWaitingThreads() {
        return null;
    }

    public int keySize() {
        return 1;
    }

    public boolean exists(K key) {
        return false;
    }

    public void clearObjects(K key) throws Exception {

    }

    public void clearObjects(K key, boolean forceRecycleBorrowed) throws Exception {

    }

    public void deleteKey(K key) throws Exception {

    }

    public void deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {

    }

    public boolean isEnabledLogPrint(K key) throws Exception {
        return false;
    }

    public void enableLogPrint(K key, boolean enable) throws Exception {

    }

    public List<Thread> interruptWaitingThreads(K key) throws Exception {
        return null;
    }

    public BeeObjectKeyPoolMonitorVo<K> getPoolMonitorVo(boolean keyMonitor) {
        return null;
    }

    public BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key) throws Exception {
        return null;
    }


    public boolean isEnabledMethodExecutionLogCache() {
        return false;
    }

    public void enableMethodExecutionLogCache(boolean enable) {

    }

    public void setMethodExecutionListener(BeeMethodExecutionListener<K> listener) {

    }

    public List<BeeMethodExecutionLog<K>> getMethodExecutionLogs(K key, int type) {
        return null;
    }

    public List<BeeMethodExecutionLog<K>> clearMethodExecutionLogs(K key, int type) {
        return null;
    }

    public List<BeeMethodExecutionLog<K>> getMethodExecutionLogs(int type) {
        return null;
    }

    public List<BeeMethodExecutionLog<K>> clearMethodExecutionLogs(int type) {
        return null;
    }
}
