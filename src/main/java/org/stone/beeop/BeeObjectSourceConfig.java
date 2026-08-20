/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;


import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.tools.exception.BeanException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.BeanUtil.*;
import static org.stone.tools.CommonUtil.*;
import static org.stone.tools.LogPrinter.DefaultLogPrinter;

/**
 * Bee object source configuration object,which is not thread-safe.
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfig<K, V> implements BeeObjectSourceConfigMXBean {
    //An atomic integer to generate sequence value as suffix of a pool name,its value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger();

    //********************************************** 1: Configuration of Pool(8) *************************************//
    //1: Pool name,default is none; if not set,a name generated with {@code PoolNameIndex} to it
    private String poolName;
    //2: Max size of poolable keys(a pooled key matches an object bucket) ,default is 10
    private int maxKeySize = 10;
    //3: A flag to enable Jmx registration,default is false
    private boolean registerMbeans;
    //4: A flag to register a jvm hook to close pool when JVM exits
    private boolean registerJvmHook = true;
    //5: A flag to enable runtime log printer of pool and log printer of buckets,default is false
    private boolean printRuntimeLogs;
    //6: A flag to enable configuration log print during pool initializes,default is false
    private boolean printConfiguration;
    //7: An exclusion list of configuration print,default is null
    private List<String> exclusionListOfPrint;
    //8: Class name of pool implementation,default is {@code KeyedObjectPool}
    private String poolImplementClassName;

    //********************************************** 2: Configuration of Bucket(14) ***********************************//
    //9: Object getting mode in pool
    private boolean fairMode;
    //10: Object creation size during pool initialization,default is zero
    private int initialSize;
    //11: Max reachable size of pooled objects of per category,pool total capacity = maxKeySize * maxActive
    private int maxActive = Math.min(Math.max(10, NCPU), 50);
    //12: Permit size of semaphore for per object category
    private int semaphoreSize = Math.min(this.maxActive / 2, NCPU);
    //13: A flag,true is that pool use a threadLocal to store used object for borrowers(false can be used to support virtual threads)
    private boolean useThreadLocal = true;
    //14: Milliseconds,max wait time for a borrower to get an object from pool,default is 8000 milliseconds(8 seconds)
    private long maxWait = 8000L;
    //15: A flag of object creation,true that pool use a thread to create initial objects during initialization,default is false
    private boolean asyncCreateInitObjects;
    //16: Milliseconds,max idle time of pooled objects stay in pool,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = 180000L;
    //17: Milliseconds: max inactive time of borrowed objects,which are recycled when timeout;default is zero,this parameter disabled
    private long holdTimeout;
    //18: Milliseconds: an interval time of pool thread to find out timeout objects(idle timeout and hold timeout),default is 18000 milliseconds(3 minutes)
    private long intervalOfClearTimeout = 180000L;
    //19: Seconds,max wait time to get alive test result on borrowed objects,default is 3 seconds.
    private int aliveTestTimeout = 3;
    //20: Milliseconds,a threshold time of alive since from last test,if gap time is less than it,assume objects are alive,and skip test,default is 500 milliseconds
    private long aliveAssumeTime = 500L;
    //21: A flag that how to close borrowed objects when pool close or pool clean,true is that pool recycles them immediately,false that pool wait them return to pool,default is false.
    private boolean forceRecycleBorrowedOnClose;
    //22: Milliseconds,wait time for pool to wait borrowed objects return to pool during pool close or pool clear,default is 3000 milliseconds
    private long parkTimeForRetry = 3000L;

    //********************************************** 3: Configuration of Pooled Objects(8) ****************************//
    //23: A list of names of methods when them be called(last access time update,exception eviction test,method execution logs)
    private List<String> objectMethodNameList;
    //24: Object factory,priority order: instance > class > class name
    private BeeObjectFactory<K, V> objectFactory;
    //25: Class of object factory
    private Class<? extends BeeObjectFactory<K, V>> objectFactoryClass;
    //26: Class name of object factory
    private String objectFactoryClassName;
    //27: A map stores some properties of object factory,these properties injected to factory during pool initialization
    private Map<String, Object> objectFactoryProperties;

    //28: Predicate to do eviction test on exception objects,priority order: instance > class > class name
    private BeeObjectPredicate predicate;
    //29: Class of predicate
    private Class<? extends BeeObjectPredicate> predicateClass;
    //30: Class name of predicate
    private String predicateClassName;

    //********************************************** 4: Configuration of method log Cache(12) *************************//
    //31: A flag to enable method log cache
    private boolean enableLogCache;
    //32: Capacity of method logs cache，default is 1000
    private int logCacheSize = 1000;
    //33: Log timeout in manager,default is 3 minutes
    private long logTimeout = 180000L;
    //34: Timer interval to clear timeout logs,default is 3 minutes
    private long intervalOfClearTimeoutLogs = logTimeout;

    //35: Slow threshold of pooled object get,default is 8000L,time unit:milliseconds,not greater than value of{@code maxWait}
    private long slowGetThreshold = 8000L;
    //36: Slow threshold of object call,default is 30 seconds,time unit:milliseconds
    private long slowCallThreshold = 30000L;
    //37: A flag to interrupt threads of slow call,default is false(not interrupted)
    private boolean interruptSlowCall;

    //38: method execution listener: instance > class > class name
    private BeeMethodLogListener<K> logListener;
    //39: Class of method execution listener,default is none
    private Class<? extends BeeMethodLogListener<K>> logListenerClass;
    //40: Class name of method execution listener,default is none
    private String logListenerClassName;

    //41: method execution listener factory: instance > class > class name
    private BeeMethodLogListenerFactory<K> logListenerFactory;
    //42: Class of method execution listener factory ,default is none
    private Class<? extends BeeMethodLogListenerFactory<K>> logListenerFactoryClass;
    //43: Class name of method execution listener factory,default is none
    private String logListenerFactoryClassName;

    //***************************************************************************************************************//
    //                                     1: constructors(4)                                                        //
    //***************************************************************************************************************//
    public BeeObjectSourceConfig() {
    }

    public BeeObjectSourceConfig(File propertiesFile) {
        load(propertiesFile);
    }

    public BeeObjectSourceConfig(String propertiesFileName) {
        load(propertiesFileName);
    }

    public BeeObjectSourceConfig(Properties configProperties) {
        load(configProperties);
    }

    //***************************************************************************************************************//
    //                                     2: Pool Configuration [1-8](18)                                           //
    //***************************************************************************************************************//
    @Override
    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public int getMaxKeySize() {
        return maxKeySize;
    }

    public void setMaxKeySize(int maxKeySize) {
        if (maxKeySize <= 0)
            throw new BeeObjectSourceConfigException("The given value of 'max-key-size' must be greater than zero");
        this.maxKeySize = maxKeySize;
    }

    @Override
    public boolean isRegisterMbeans() {
        return registerMbeans;
    }

    public void setRegisterMbeans(boolean registerMbeans) {
        this.registerMbeans = registerMbeans;
    }

    public boolean isRegisterJvmHook() {
        return registerJvmHook;
    }

    public void setRegisterJvmHook(boolean registerJvmHook) {
        this.registerJvmHook = registerJvmHook;
    }

    public boolean isPrintRuntimeLogs() {
        return printRuntimeLogs;
    }

    public void setPrintRuntimeLogs(boolean printRuntimeLogs) {
        this.printRuntimeLogs = printRuntimeLogs;
    }

    public boolean isPrintConfiguration() {
        return printConfiguration;
    }

    public void setPrintConfiguration(boolean printConfiguration) {
        this.printConfiguration = printConfiguration;
    }

    public void addExclusionNameOfPrint(String fieldName) {
        if (exclusionListOfPrint == null)
            this.exclusionListOfPrint = new ArrayList<>(1);

        if (!exclusionListOfPrint.contains(fieldName))
            this.exclusionListOfPrint.add(fieldName);
    }

    public boolean removeExclusionNameOfPrint(String fieldName) {
        return exclusionListOfPrint != null && exclusionListOfPrint.remove(fieldName);
    }

    public boolean existExclusionNameOfPrint(String fieldName) {
        return exclusionListOfPrint != null && exclusionListOfPrint.contains(fieldName);
    }

    public void clearExclusionListOfPrint() {
        if (exclusionListOfPrint != null) this.exclusionListOfPrint.clear();
    }

    @Override
    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        this.poolImplementClassName = poolImplementClassName;
    }

    //***************************************************************************************************************//
    //                                     3: Bucket Configuration[9-22](28)                                         //
    //***************************************************************************************************************//
    @Override
    public boolean isFairMode() {
        return fairMode;
    }

    public void setFairMode(boolean fairMode) {
        this.fairMode = fairMode;
    }

    @Override
    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        if (initialSize < 0)
            throw new BeeObjectSourceConfigException("The given value of 'initial-size' cannot be less than zero");
        this.initialSize = initialSize;
    }

    @Override
    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (maxActive <= 0)
            throw new BeeObjectSourceConfigException("The given value of 'max-active' must be greater than zero");
        this.maxActive = maxActive;
        this.semaphoreSize = (maxActive > 1) ? Math.min(maxActive / 2, NCPU) : 1;
    }

    @Override
    public int getSemaphoreSize() {
        return semaphoreSize;
    }

    public void setSemaphoreSize(int semaphoreSize) {
        if (semaphoreSize <= 0)
            throw new BeeObjectSourceConfigException("The given value of 'borrow-semaphore-size' must be greater than zero");
        this.semaphoreSize = semaphoreSize;
    }

    public boolean isUseThreadLocal() {
        return useThreadLocal;
    }

    public void setUseThreadLocal(boolean useThreadLocal) {
        this.useThreadLocal = useThreadLocal;
    }

    @Override
    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        if (maxWait <= 0L)
            throw new BeeObjectSourceConfigException("The given value of 'max-wait' must be greater than zero");
        this.maxWait = maxWait;
        if (this.slowGetThreshold > maxWait) this.slowGetThreshold = maxWait;
    }

    public boolean isAsyncCreateInitObjects() {
        return asyncCreateInitObjects;
    }

    public void setAsyncCreateInitObjects(boolean asyncCreateInitObjects) {
        this.asyncCreateInitObjects = asyncCreateInitObjects;
    }

    @Override
    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout <= 0L)
            throw new BeeObjectSourceConfigException("The given value of 'idle-timeout' must be greater than zero");
        this.idleTimeout = idleTimeout;
    }

    @Override
    public long getHoldTimeout() {
        return holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout < 0L)
            throw new BeeObjectSourceConfigException("The given value of 'hold-timeout' cannot be less than zero");
        this.holdTimeout = holdTimeout;
    }

    @Override
    public long getIntervalOfClearTimeout() {
        return intervalOfClearTimeout;
    }

    public void setIntervalOfClearTimeout(long intervalOfClearTimeout) {
        if (intervalOfClearTimeout <= 0L)
            throw new BeeObjectSourceConfigException("The given value of 'interval-of-clear-timeout' must be greater than zero");
        this.intervalOfClearTimeout = intervalOfClearTimeout;
    }

    @Override
    public int getAliveTestTimeout() {
        return aliveTestTimeout;
    }

    public void setAliveTestTimeout(int aliveTestTimeout) {
        if (aliveTestTimeout < 0L)
            throw new BeeObjectSourceConfigException("The given value of 'alive-test-timeout' cannot  be less than zero");
        this.aliveTestTimeout = aliveTestTimeout;
    }

    @Override
    public long getAliveAssumeTime() {
        return aliveAssumeTime;
    }

    public void setAliveAssumeTime(long aliveAssumeTime) {
        if (aliveAssumeTime < 0L)
            throw new BeeObjectSourceConfigException("The given value of 'alive-assume-time' cannot be less than zero");
        this.aliveAssumeTime = aliveAssumeTime;
    }

    @Override
    public boolean isForceRecycleBorrowedOnClose() {
        return forceRecycleBorrowedOnClose;
    }

    public void setForceRecycleBorrowedOnClose(boolean forceRecycleBorrowedOnClose) {
        this.forceRecycleBorrowedOnClose = forceRecycleBorrowedOnClose;
    }

    @Override
    public long getParkTimeForRetry() {
        return parkTimeForRetry;
    }

    public void setParkTimeForRetry(long parkTimeForRetry) {
        if (parkTimeForRetry < 0L)
            throw new BeeObjectSourceConfigException("The given value of 'park-time-for-retry' cannot be less than zero");
        this.parkTimeForRetry = parkTimeForRetry;
    }

    //***************************************************************************************************************//
    //                                     4: Configuration of Pooled Objects[23-30](19)                             //
    //***************************************************************************************************************//
    public List<String> getObjectMethodNameList() {
        return this.objectMethodNameList;
    }

    public void removeObjectMethodName(String methodName) {
        if (this.objectMethodNameList != null) objectMethodNameList.remove(methodName);
    }

    public void addObjectMethodName(String methodName) {
        if (isBlank(methodName))
            throw new BeeObjectSourceConfigException("The given value of 'method-name' can't be null or blank");
        if (this.objectMethodNameList == null) this.objectMethodNameList = new ArrayList<>(1);
        if (!objectMethodNameList.contains(methodName)) objectMethodNameList.add(methodName);
    }

    public BeeObjectFactory<K, V> getObjectFactory() {
        return this.objectFactory;
    }

    public void setObjectFactory(BeeObjectFactory<K, V> factory) {
        this.objectFactory = factory;
    }

    public Class<? extends BeeObjectFactory<K, V>> getObjectFactoryClass() {
        return this.objectFactoryClass;
    }

    public void setObjectFactoryClass(Class<? extends BeeObjectFactory<K, V>> objectFactoryClass) {
        this.objectFactoryClass = objectFactoryClass;
    }

    public String getObjectFactoryClassName() {
        return this.objectFactoryClassName;
    }

    public void setObjectFactoryClassName(String objectFactoryClassName) {
        this.objectFactoryClassName = trimString(objectFactoryClassName);
    }

    public BeeObjectPredicate getPredicate() {
        return predicate;
    }

    public void setPredicate(BeeObjectPredicate predicate) {
        this.predicate = predicate;
    }

    public Class<? extends BeeObjectPredicate> getPredicateClass() {
        return predicateClass;
    }

    public void setPredicateClass(Class<? extends BeeObjectPredicate> predicateClass) {
        this.predicateClass = predicateClass;
    }

    public String getPredicateClassName() {
        return predicateClassName;
    }

    public void setPredicateClassName(String predicateClassName) {
        this.predicateClassName = predicateClassName;
    }

    public Object getObjectFactoryProperty(String key) {
        return objectFactoryProperties != null ? this.objectFactoryProperties.get(key) : null;
    }

    public Object removeObjectFactoryProperty(String key) {
        return objectFactoryProperties != null ? this.objectFactoryProperties.remove(key) : null;
    }

    public void addObjectFactoryProperty(String key, Object value) {
        if (isNotBlank(key) && value != null) {
            if (objectFactoryProperties == null) objectFactoryProperties = new HashMap<>(1);
            this.objectFactoryProperties.put(key, value);
        }
    }

    public void addObjectFactoryProperty(String propertyText) {
        if (isNotBlank(propertyText)) {
            if (objectFactoryProperties == null) objectFactoryProperties = new HashMap<>(1);
            String[] attributeArray = propertyText.split("&");
            for (String attribute : attributeArray) {
                String[] pair = attribute.split("=");
                if (pair.length == 2) {
                    this.objectFactoryProperties.put(pair[0].trim(), pair[1].trim());
                } else {
                    pair = attribute.split(":");
                    if (pair.length == 2) {
                        this.objectFactoryProperties.put(pair[0].trim(), pair[1].trim());
                    }
                }
            }
        }
    }

    //****************************************************************************************************************//
    //                                    5: Configuration of method log Cache[31-42](24)                             //
    //****************************************************************************************************************//
    public boolean isEnableLogCache() {
        return enableLogCache;
    }

    public void setEnableLogCache(boolean enableLogCache) {
        this.enableLogCache = enableLogCache;
    }

    public int getLogCacheSize() {
        return logCacheSize;
    }

    public void setLogCacheSize(int logCacheSize) {
        if (logCacheSize <= 0)
            throw new BeeObjectSourceConfigException("The given value of 'log-cache-size' must be greater than zero");
        this.logCacheSize = logCacheSize;
    }

    public long getSlowGetThreshold() {
        return slowGetThreshold;
    }

    public void setSlowGetThreshold(long slowGetThreshold) {
        if (slowGetThreshold <= 0L)
            throw new BeeObjectSourceConfigException("The given value of 'slow-get-threshold' must be greater than zero");
        if (slowGetThreshold > this.maxWait)
            throw new BeeObjectSourceConfigException("The given value of 'slow-get-threshold' cannot be greater than 'max-wait'");

        this.slowGetThreshold = slowGetThreshold;
    }

    public long getSlowCallThreshold() {
        return slowCallThreshold;
    }

    public void setSlowCallThreshold(long slowCallThreshold) {
        if (slowCallThreshold <= 0L)
            throw new BeeObjectSourceConfigException("The given value of 'slow-call-threshold' must be greater than zero");

        this.slowCallThreshold = slowCallThreshold;
    }

    public boolean isInterruptSlowCall() {
        return interruptSlowCall;
    }

    public void setInterruptSlowCall(boolean interruptSlowCall) {
        this.interruptSlowCall = interruptSlowCall;
    }

    public long getLogTimeout() {
        return logTimeout;
    }

    public void setLogTimeout(long logTimeout) {
        if (logTimeout <= 0L)
            throw new BeeObjectSourceConfigException("The given value of 'log-timeout' must be greater than zero");
        this.logTimeout = logTimeout;
    }

    public long getIntervalOfClearTimeoutLogs() {
        return intervalOfClearTimeoutLogs;
    }

    public void setIntervalOfClearTimeoutLogs(long intervalOfClearTimeoutLogs) {
        if (intervalOfClearTimeoutLogs <= 0L)
            throw new BeeObjectSourceConfigException("The given value of 'interval-of-clear-timeout-execution-logs' must be greater than zero");
        this.intervalOfClearTimeoutLogs = intervalOfClearTimeoutLogs;
    }

    public BeeMethodLogListener<K> getLogListener() {
        return logListener;
    }

    public void setLogListener(BeeMethodLogListener<K> logListener) {
        this.logListener = logListener;
    }

    public Class<? extends BeeMethodLogListener<K>> getLogListenerClass() {
        return logListenerClass;
    }

    public void setLogListenerClass(Class<? extends BeeMethodLogListener<K>> logListenerClass) {
        this.logListenerClass = logListenerClass;
    }

    public String getLogListenerClassName() {
        return logListenerClassName;
    }

    public void setLogListenerClassName(String logListenerClassName) {
        this.logListenerClassName = logListenerClassName;
    }

    public BeeMethodLogListenerFactory<K> getLogListenerFactory() {
        return logListenerFactory;
    }

    public void setLogListenerFactory(BeeMethodLogListenerFactory<K> logListenerFactory) {
        this.logListenerFactory = logListenerFactory;
    }

    public Class<? extends BeeMethodLogListenerFactory<K>> getLogListenerFactoryClass() {
        return logListenerFactoryClass;
    }

    public void setLogListenerFactoryClass(Class<? extends BeeMethodLogListenerFactory<K>> logListenerFactoryClass) {
        this.logListenerFactoryClass = logListenerFactoryClass;
    }

    public String getLogListenerFactoryClassName() {
        return logListenerFactoryClassName;
    }

    public void setLogListenerFactoryClassName(String logListenerFactoryClassName) {
        this.logListenerFactoryClassName = logListenerFactoryClassName;
    }

    //***************************************************************************************************************//
    //                                     6: load from file                                                         //
    //***************************************************************************************************************//
    public void load(String filename) {
        load(filename, null);
    }

    public void load(String filename, String keyPrefix) {
        if (isBlank(filename))
            throw new BeeObjectSourceConfigException("Load file name cannot be null or empty");
        String fileLowerCaseName = filename.toLowerCase(Locale.US);
        if (!fileLowerCaseName.endsWith(".properties"))
            throw new BeeObjectSourceConfigException("Load file extension name must be 'properties':" + filename);

        if (fileLowerCaseName.startsWith("cp:")) {//1:'cp:' prefix
            String cpFileName = fileLowerCaseName.substring("cp:".length());
            Properties fileProperties = loadPropertiesFromClassPathFile(cpFileName);
            load(fileProperties, keyPrefix);
        } else if (fileLowerCaseName.startsWith("classpath:")) {//2:'classpath:' prefix
            String cpFileName = fileLowerCaseName.substring("classpath:".length());
            Properties fileProperties = loadPropertiesFromClassPathFile(cpFileName);
            load(fileProperties, keyPrefix);
        } else {
            load(new File(filename), keyPrefix);
        }
    }

    //***************************************************************************************************************//
    //                                     7: load from file                                                         //
    //***************************************************************************************************************//
    public void load(File file) {
        load(file, null);
    }

    public void load(File file, String keyPrefix) {
        if (file == null) throw new BeeObjectSourceConfigException("Load file cannot be null");
        if (!file.exists()) throw new BeeObjectSourceConfigException("Load file not found:(" + file + ")");
        if (!file.isFile()) throw new BeeObjectSourceConfigException("Load file cannot be a folder:(" + file + ")");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new BeeObjectSourceConfigException("Load file extension name must be 'properties':(" + file + ")");

        try (InputStream stream = Files.newInputStream(file.toPath())) {
            Properties configProperties = new Properties();
            configProperties.load(stream);
            this.load(configProperties, keyPrefix);
        } catch (IOException e) {
            throw new BeeObjectSourceConfigException("Failed to load configuration file:" + file, e);
        }
    }

    //***************************************************************************************************************//
    //                                     8: load from map                                                          //
    //***************************************************************************************************************//
    public void load(Properties configProperties) {
        load(configProperties, null);
    }

    public void load(Properties configProperties, String keyPrefix) {
        if (configProperties == null || configProperties.isEmpty())
            throw new BeeObjectSourceConfigException("Load properties cannot be null or empty");

        Map<String, Object> configMap = new HashMap<>(configProperties.size());
        for (Map.Entry<Object, Object> entry : configProperties.entrySet()) {
            if (entry.getKey() instanceof String) {
                configMap.put((String) entry.getKey(), entry.getValue());
            }
        }
        load(configMap, keyPrefix);
    }

    //***************************************************************************************************************//
    //                                     9: load from map                                                          //
    //***************************************************************************************************************//
    public void load(Map<String, Object> configMap) {
        load(configMap, null);
    }

    public void load(Map<String, Object> configMap, String keyPrefix) {
        if (configMap == null || configMap.isEmpty())
            throw new BeeObjectSourceConfigException("Load map cannot be null or empty");

        //1: load configuration item values from outside properties
        HashMap<String, Object> setValueMap;
        if (isNotBlank(keyPrefix)) {
            if (keyPrefix.charAt(keyPrefix.length() - 1) != '.') keyPrefix = keyPrefix + ".";
            final int keyPrefixLen = keyPrefix.length();
            setValueMap = new HashMap<>(configMap.size());
            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                if (entry.getKey().startsWith(keyPrefix)) {
                    setValueMap.put(entry.getKey().substring(keyPrefixLen), entry.getValue());
                }
            }
        } else {
            setValueMap = new HashMap<>(configMap);
        }

        //2: remove some special keys in setValueMap
        Object factoryPropertiesValue = setValueMap.remove(CONFIG_FACTORY_PROP);
        Object factoryPropertiesSizeValue = setValueMap.remove(CONFIG_FACTORY_PROP_SIZE);
        Object exclusionListOfPrintValue = setValueMap.remove(CONFIG_EXCLUSION_LIST_OF_PRINT);
        Object objectMethodNameValue = setValueMap.remove(CONFIG_OBJECT_METHOD_LIST);

        //3:inject item value from map to this dataSource config object
        try {
            setPropertiesValue(this, setValueMap);
        } catch (BeanException e) {
            throw new BeeObjectSourceConfigException(e.getMessage(), e);
        }

        //4:try to find 'factoryProperties' config value
        if (factoryPropertiesValue instanceof String)
            this.addObjectFactoryProperty((String) factoryPropertiesValue);
        int factoryPropertiesSize = 0;
        if (factoryPropertiesSizeValue instanceof String) {
            factoryPropertiesSize = Integer.parseInt(((String) factoryPropertiesSizeValue).trim());
        } else if (factoryPropertiesSizeValue instanceof Number) {
            factoryPropertiesSize = ((Number) factoryPropertiesSizeValue).intValue();
        }
        if (factoryPropertiesSize > 0) {
            for (int i = 1; i <= factoryPropertiesSize; i++) {//properties index begin with 1
                Object factoryProperty = getPropertyValue(setValueMap, CONFIG_FACTORY_PROP_KEY_PREFIX + i);
                if (factoryProperty instanceof String)
                    this.addObjectFactoryProperty((String) factoryProperty);
            }
        }

        //5:try to load exclusion list on config print
        if (exclusionListOfPrintValue instanceof String) {
            this.clearExclusionListOfPrint();//remove existed exclusion
            for (String exclusion : ((String) exclusionListOfPrintValue).trim().split(",")) {
                this.addExclusionNameOfPrint(exclusion);
            }
        }

        //6:object method name list
        if (objectMethodNameValue instanceof String) {
            for (String methodName : ((String) objectMethodNameValue).trim().split(",")) {
                this.addObjectMethodName(methodName);
            }
        }
    }

    //***************************************************************************************************************//
    //                                     7: configuration check and object factory creation(1+4)                   //
    //***************************************************************************************************************//
    //check pool configuration
    public BeeObjectSourceConfig<K, V> check() {
        if (initialSize > this.maxActive)
            throw new BeeObjectSourceConfigException("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'");

        //1: try to create object factory
        BeeObjectFactory<K, V> objectFactory = this.createObjectFactory();

        //2: create predicate and filter
        BeeObjectPredicate predicate = this.createObjectPredicate();
        //4: create a method log listener
        BeeMethodLogListener<K> methodExecutionListener = this.createLogListener();
        //5: create a copy from this current configuration object
        BeeObjectSourceConfig<K, V> checkedConfig = new BeeObjectSourceConfig<>();
        copyTo(checkedConfig);

        //6: assign above objects to the checked configuration object(such as factory,filter,predicate)
        checkedConfig.objectFactory = objectFactory;
        if (predicate != null) checkedConfig.predicate = predicate;
        if (methodExecutionListener != null) checkedConfig.logListener = methodExecutionListener;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "KeyPool-" + PoolNameIndex.incrementAndGet();
        if (checkedConfig.printConfiguration) printConfiguration(checkedConfig);
        return checkedConfig;
    }

    void copyTo(BeeObjectSourceConfig<K, V> config) {
        String fieldName = "";
        try {
            for (Field field : BeeObjectSourceConfig.class.getDeclaredFields()) {
                fieldName = field.getName();
                switch (fieldName) {
                    case CONFIG_POOL_NAME_INDEX:
                        break;
                    case CONFIG_FACTORY_PROP:
                        if (objectFactoryProperties != null && !objectFactoryProperties.isEmpty()) {
                            if (config.objectFactoryProperties == null) {
                                config.objectFactoryProperties = new HashMap<>(objectFactoryProperties);
                            } else {
                                config.objectFactoryProperties.clear();
                                config.objectFactoryProperties.putAll(objectFactoryProperties);
                            }
                        }
                        break;
                    case CONFIG_EXCLUSION_LIST_OF_PRINT:
                        if (exclusionListOfPrint != null && !exclusionListOfPrint.isEmpty())
                            config.exclusionListOfPrint = new ArrayList<>(exclusionListOfPrint);
                        break;
                    case CONFIG_OBJECT_METHOD_LIST:
                        if (this.objectMethodNameList != null && !objectMethodNameList.isEmpty())
                            config.objectMethodNameList = new ArrayList<>(objectMethodNameList);
                        break;
                    default: //other config items
                        field.set(config, field.get(this));
                }
            }
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to set value on field[" + fieldName + "]", e);
        }
    }

    private BeeObjectFactory<K, V> createObjectFactory() {
        //1: copy from member field of configuration
        BeeObjectFactory<K, V> objectFactory = this.objectFactory;

        //2: create factory instance
        if (objectFactory == null && (objectFactoryClass != null || objectFactoryClassName != null)) {
            Class<? extends BeeObjectFactory<K, V>> factoryClass = null;
            try {
                factoryClass = objectFactoryClass != null ? objectFactoryClass : loadClass(objectFactoryClassName);
                objectFactory = createClassInstance(factoryClass, BeeObjectFactory.class, "object factory");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object factory class:" + objectFactoryClassName, e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object factory by class:" + factoryClass, e);
            }
        }

        //3: Throws exception if not configured factory
        if (objectFactory == null)
            throw new BeeObjectSourceConfigException("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]");
        //4: Throws exception if default key is null
        if (objectFactory.getDefaultKey() == null)
            throw new BeeObjectSourceConfigException("Object factory must provide a non null default pooled key");

        //5: Injects properties to factory
        if (objectFactoryProperties != null && !objectFactoryProperties.isEmpty())
            try {
                setPropertiesValue(objectFactory, objectFactoryProperties);
            } catch (BeanException e) {
                throw new BeeObjectSourceConfigException(e.getMessage(), e);
            }

        return objectFactory;
    }

    private BeeObjectPredicate createObjectPredicate() throws BeeObjectSourceConfigException {
        //step1:if exits a set predicate,then return it
        if (this.predicate != null) return this.predicate;

        //step2: create predicate instance with a class or class name
        if (predicateClass != null || isNotBlank(predicateClassName)) {
            Class<? extends BeeObjectPredicate> predicationClass = null;
            try {
                predicationClass = predicateClass != null ? predicateClass : loadClass(predicateClassName);
                return createClassInstance(predicationClass, BeeObjectPredicate.class, "object predicate");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found predicate class:" + predicateClassName, e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create predicate instance with class:" + predicationClass, e);
            }
        }
        return null;
    }

    //create object call log handler
    private BeeMethodLogListener<K> createLogListener() {
        //step1:if exists handler,then return it
        if (this.logListener != null) return this.logListener;

        //step2:if exists listener factory,then use it to create one
        if (this.logListenerFactory != null) {
            try {
                return logListenerFactory.create(this);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create log listener by factory", e);
            }
        }

        //step3: create listener factory and let it create a listener
        if (this.logListenerFactoryClass != null || isNotBlank(this.logListenerFactoryClassName)) {
            BeeMethodLogListenerFactory<K> factory;
            Class<? extends BeeMethodLogListenerFactory<K>> listenerFactoryClass = null;
            try {
                listenerFactoryClass = logListenerFactoryClass != null ? logListenerFactoryClass : loadClass(logListenerFactoryClassName);
                factory = createClassInstance(listenerFactoryClass, BeeMethodLogListenerFactory.class, "method execution listener factory");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Failed to create log listener factory with class:" + logListenerClassName, e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create log listener factory with class:" + listenerFactoryClass, e);
            }

            try {
                return factory.create(this);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create log listener by factory", e);
            }
        }

        //step4: create a listener
        if (this.logListenerClass != null || isNotBlank(this.logListenerClassName)) {
            Class<? extends BeeMethodLogListener<K>> listenerClass = null;
            try {
                listenerClass = logListenerClass != null ? logListenerClass : loadClass(logListenerClassName);
                return createClassInstance(listenerClass, BeeMethodLogListener.class, "object method execution listener");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Failed to create log listener with class:" + logListenerClassName, e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create log listener with class:" + listenerClass, e);
            }
        }
        return null;
    }

    //print check passed configuration
    private void printConfiguration(BeeObjectSourceConfig<K, V> checkedConfig) {
        String poolName = checkedConfig.poolName;
        List<String> exclusionList = checkedConfig.exclusionListOfPrint;
        if (exclusionList == null) exclusionList = Collections.emptyList();
        DefaultLogPrinter.info("................................................BeeOP({})-configuration[start]................................................", poolName);

        try {
            for (Field field : BeeObjectSourceConfig.class.getDeclaredFields()) {
                String fieldName = field.getName();
                boolean infoPrint = !exclusionList.contains(fieldName);

                switch (fieldName) {
                    case CONFIG_POOL_NAME_INDEX:
                    case CONFIG_EXCLUSION_LIST_OF_PRINT:
                        break;
                    case CONFIG_FACTORY_PROP: {
                        if (this.objectFactoryProperties != null && !this.objectFactoryProperties.isEmpty()) {
                            if (infoPrint) {
                                for (Map.Entry<String, Object> entry : checkedConfig.objectFactoryProperties.entrySet())
                                    if (!exclusionList.contains(entry.getKey())) {
                                        DefaultLogPrinter.info("BeeOP({})-config.objectFactoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                                    } else {
                                        DefaultLogPrinter.debug("BeeOP({})-config.objectFactoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                                    }
                            } else {
                                for (Map.Entry<String, Object> entry : checkedConfig.objectFactoryProperties.entrySet())
                                    DefaultLogPrinter.debug("BeeOP({})-config.objectFactoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            }
                        }
                        break;
                    }

                    default:
                        if (infoPrint)
                            DefaultLogPrinter.info("BeeOP({})-config.{}={}", poolName, fieldName, field.get(checkedConfig));
                        else
                            DefaultLogPrinter.debug("BeeOP({})-config.{}={}", poolName, fieldName, field.get(checkedConfig));
                }
            }
        } catch (Throwable e) {
            DefaultLogPrinter.warn("BeeOP({})-failed to print configuration", poolName, e);
        }
        DefaultLogPrinter.info("................................................BeeOP({})-configuration[end]................................................", poolName);
    }
}

