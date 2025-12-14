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

    public void enableLogPrint(boolean enable) {

    }

    public List<Thread> interruptWaitingThreads() throws Exception {
        return null;
    }

    public int keySize() {
        return 1;
    }

    public boolean exists(K key) {
        return false;
    }

    public void reset(K key) throws Exception {

    }

    public void reset(K key, boolean forceRecycleBorrowed) throws Exception {

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

    public BeeObjectPoolMonitorVo getMonitorVo(K key) throws Exception {
        return null;
    }


    public boolean isEnabledMethodExecutionLogCache() {
        return false;
    }

    public void enableMethodExecutionLogCache(boolean enable) {

    }

    public void setMethodExecutionListener(BeeMethodExecutionListener<K, V> listener) {

    }

    public List<BeeMethodExecutionLog<K, V>> getMethodExecutionLog(K key,int type) {
        return null;
    }

    public List<BeeMethodExecutionLog<K, V>> clearMethodExecutionLog(K key,int type) {
        return null;
    }

    public List<BeeMethodExecutionLog<K, V>> getAllMethodExecutionLog(int type) {
        return null;
    }

    public List<BeeMethodExecutionLog<K, V>> clearAllMethodExecutionLog(int type) {
        return null;
    }
}
