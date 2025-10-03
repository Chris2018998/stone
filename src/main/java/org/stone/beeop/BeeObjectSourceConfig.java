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

import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beeop.pool.KeyedObjectPool;
import org.stone.tools.CommonUtil;
import org.stone.tools.exception.BeanException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.BeanUtil.*;
import static org.stone.tools.CommonUtil.*;

/**
 * Bee object source configuration object
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfig<K, V> implements BeeObjectSourceConfigMBean {
    //An atomic integer to generate sequence value as suffix of a pool name,its value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //A map stores some properties of object factory,these properties injected to factory during pool initialization
    private final Map<String, Object> factoryProperties = new HashMap<>(0);

    //Pool name,default is none; if not set,a name generated with {@code PoolNameIndex} for it
    private String poolName;
    //Object getting mode in pool
    private boolean fairMode;
    //Object creation size during pool initialization,default is zero
    private int initialSize;
    //Max reachable size of object categories in pool,default is 50
    private int maxKeySize = 50;
    //Max reachable size of pooled objects of per category,pool total capacity = maxObjectKeySize * maxActive
    private int maxActive = Math.min(Math.max(10, CommonUtil.NCPU), 50);
    //Permit size of semaphore for per object category
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, CommonUtil.NCPU);
    //Milliseconds: max wait time for a borrower to get a object from pool,default is 8000 milliseconds(8 seconds)
    private long maxWait = SECONDS.toMillis(8L);
    //An indicator of object creation,true that pool use a thread to create initial objects during initialization,default is false
    private boolean asyncCreateInitObject;

    //Milliseconds: max idle time of pooled objects stay in pool,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = MINUTES.toMillis(3L);
    //Milliseconds: max inactive time of borrowed objects,which are recycled when timeout;default is zero,this parameter disabled
    private long holdTimeout;

    //Seconds: max wait time to get alive test result on borrowed objects,default is 3 seconds.
    private int aliveTestTimeout = 3;
    //Milliseconds: a threshold time of alive since from last test,if gap time is less than it,assume objects are alive,and skip test,default is 500 milliseconds
    private long aliveAssumeTime = 500L;
    //Milliseconds: an interval time of pool thread to find out timeout objects(idle timeout and hold timeout),default is 18000 milliseconds(3 minutes)
    private long timerCheckInterval = MINUTES.toMillis(3L);
    //An indicator that how to close borrowed objects when pool close or pool clean,true is that pool recycles them immediately,false that pool wait them return to pool,default is false.
    private boolean forceRecycleBorrowedOnClose;
    //An indicator that shutdown thread pool when restart or shutdown object pool.
    private boolean forceShutdownThreadPoolOnClose;
    //Milliseconds: wait time for pool to wait borrowed objects return to pool during pool close or pool clear,default is 3000 milliseconds
    private long parkTimeForRetry = 3000L;

    //An indicator,true is that pool use a threadLocal to store used object for borrowers(false can be used to support virtual threads)
    private boolean enableThreadLocal = true;
    //An indicator to enable Jmx registration,default is false
    private boolean enableJmx;
    //An indicator to enable runtime log print in pool,default is false
    private boolean printRuntimeLog;
    //An indicator to enable configuration log print during pool initializes,default is false
    private boolean printConfigInfo;
    //A list of field name,not be log print during pool initialization,default is null
    private List<String> configPrintExclusionList;

    //An array of interfaces implemented by object class
    private Class<?>[] objectInterfaces;
    //A class name array of interface implemented by object class
    private String[] objectInterfaceNames;

    //Object factory to create pooled objects to pool,first priority for being used if exists
    private BeeObjectFactory<K, V> objectFactory;
    //Class of object factory,second priority for being used if exists
    private Class<? extends BeeObjectFactory<K, V>> objectFactoryClass;
    //Class name of object factory,third priority for being used if exists
    private String objectFactoryClassName;

    //Predicate to do eviction test on exception objects,first priority for selected if exists
    private BeeObjectPredicate objectPredicate;
    //Class of predicate,second priority for being used if exists
    private Class<? extends BeeObjectPredicate> objectPredicateClass;
    //Class name of predicate,third priority for being used if exists
    private String objectPredicateClassName;

    //Class name of pool implementation,default is {@code KeyedObjectPool}
    private String poolImplementClassName = KeyedObjectPool.class.getName();

    //********************************************** object call logs **************************************************//
    //slow threshold value of object get,time unit:milliseconds
    private long slowObjectGetThreshold;
    //slow threshold of object call,time unit:milliseconds
    private long slowObjectCallThreshold;
    //Capacity of method logs cache，default is 1000
    private int objectCallLogCacheSize = 1000;
    //Work mode of object call log listener,default is true,sync mode
    private boolean objectCallLogListenInSync = true;
    //log timeout in collector,default is 3 minutes
    private long objectCallLogTimeout = MINUTES.toMillis(3L);
    //timer interval to clear timeout logs
    private long objectCallLogClearInterval = objectCallLogTimeout;

    //object call log listener
    private BeeObjectCallLogListener<K, V> objectCallLogListener;
    //Class of object call log listener,default is none
    private Class<? extends BeeObjectCallLogListener<K, V>> objectCallLogListenerClass;
    //Class name of log listener,default is none
    private String objectCallLogListenerClassName;

    //object call logs collector
    private BeeObjectCallLogCollector<K, V> objectCallLogCollector;
    //Class of object call logs collector,default is none
    private Class<? extends BeeObjectCallLogCollector<K, V>> objectCallLogCollectorClass;
    //Class name of object call logs collector,default is none
    private String objectCallLogCollectorClassName;

    //***************************************************************************************************************//
    //                                     1: constructors(4)                                                        //
    //***************************************************************************************************************//
    public BeeObjectSourceConfig() {
    }

    public BeeObjectSourceConfig(File propertiesFile) {
        loadFromPropertiesFile(propertiesFile);
    }

    public BeeObjectSourceConfig(String propertiesFileName) {
        loadFromPropertiesFile(propertiesFileName);
    }

    public BeeObjectSourceConfig(Properties configProperties) {
        loadFromProperties(configProperties);
    }

    //***************************************************************************************************************//
    //                                     2: base configuration(40)                                                 //
    //***************************************************************************************************************//
    public String getPoolName() {
        return this.poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = trimString(poolName);
    }

    public boolean isFairMode() {
        return this.fairMode;
    }

    public void setFairMode(boolean fairMode) {
        this.fairMode = fairMode;
    }

    public int getInitialSize() {
        return this.initialSize;
    }

    public void setInitialSize(int initialSize) {
        if (initialSize < 0)
            throw new InvalidParameterException("The given value for the configuration item 'initial-size' cannot be less than zero");
        this.initialSize = initialSize;
    }

    public boolean isAsyncCreateInitObject() {
        return asyncCreateInitObject;
    }

    public void setAsyncCreateInitObject(boolean asyncCreateInitObject) {
        this.asyncCreateInitObject = asyncCreateInitObject;
    }

    public int getMaxActive() {
        return this.maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (maxActive <= 0)
            throw new InvalidParameterException("The given value for configuration item 'max-active' must be greater than zero");
        this.maxActive = maxActive;
        borrowSemaphoreSize = (maxActive > 1) ? Math.min(maxActive / 2, CommonUtil.NCPU) : 1;
    }

    public int getMaxKeySize() {
        return maxKeySize;
    }

    public void setMaxKeySize(int maxKeySize) {
        if (maxKeySize <= 0)
            throw new InvalidParameterException("The given value for configuration item 'max-key-size' must be greater than zero");
        this.maxKeySize = maxKeySize;
    }

    public int getBorrowSemaphoreSize() {
        return this.borrowSemaphoreSize;
    }

    public void setBorrowSemaphoreSize(int borrowSemaphoreSize) {
        if (borrowSemaphoreSize <= 0)
            throw new InvalidParameterException("The given value for configuration item 'borrow-semaphore-size' must be greater than zero");
        this.borrowSemaphoreSize = borrowSemaphoreSize;
    }

    public long getMaxWait() {
        return this.maxWait;
    }

    public void setMaxWait(long maxWait) {
        if (maxWait <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'max-wait' must be greater than zero");
        this.maxWait = maxWait;
    }

    public long getIdleTimeout() {
        return this.idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'idle-timeout' must be greater than zero");
        this.idleTimeout = idleTimeout;
    }

    public long getHoldTimeout() {
        return this.holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout < 0L)
            throw new InvalidParameterException("The given value for configuration item 'hold-timeout' cannot be less than zero");

        this.holdTimeout = holdTimeout;
    }

    public int getAliveTestTimeout() {
        return this.aliveTestTimeout;
    }

    public void setAliveTestTimeout(int aliveTestTimeout) {
        if (aliveTestTimeout < 0L)
            throw new InvalidParameterException("The given value for configuration item 'alive-test-timeout' cannot  be less than zero");
        this.aliveTestTimeout = aliveTestTimeout;
    }

    public long getAliveAssumeTime() {
        return this.aliveAssumeTime;
    }

    public void setAliveAssumeTime(long aliveAssumeTime) {
        if (aliveAssumeTime < 0L)
            throw new InvalidParameterException("The given value for configuration item 'alive-assume-time' cannot be less than zero");
        this.aliveAssumeTime = aliveAssumeTime;
    }

    public long getTimerCheckInterval() {
        return this.timerCheckInterval;
    }

    public void setTimerCheckInterval(long timerCheckInterval) {
        if (timerCheckInterval <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'timer-check-interval' must be greater than zero");
        this.timerCheckInterval = timerCheckInterval;
    }

    public boolean isForceRecycleBorrowedOnClose() {
        return this.forceRecycleBorrowedOnClose;
    }

    public void setForceRecycleBorrowedOnClose(boolean forceRecycleBorrowedOnClose) {
        this.forceRecycleBorrowedOnClose = forceRecycleBorrowedOnClose;
    }

    public boolean isForceShutdownThreadPoolOnClose() {
        return forceShutdownThreadPoolOnClose;
    }

    public void setForceShutdownThreadPoolOnClose(boolean forceShutdownThreadPoolOnClose) {
        this.forceShutdownThreadPoolOnClose = forceShutdownThreadPoolOnClose;
    }

    public long getParkTimeForRetry() {
        return this.parkTimeForRetry;
    }

    public void setParkTimeForRetry(long parkTimeForRetry) {
        if (parkTimeForRetry < 0L)
            throw new InvalidParameterException("The given value for configuration item 'park-time-for-retry' cannot be less than zero");
        this.parkTimeForRetry = parkTimeForRetry;
    }

    public boolean isEnableJmx() {
        return this.enableJmx;
    }

    public void setEnableJmx(boolean enableJmx) {
        this.enableJmx = enableJmx;
    }

    public boolean isEnableThreadLocal() {
        return enableThreadLocal;
    }

    public void setEnableThreadLocal(boolean enableThreadLocal) {
        this.enableThreadLocal = enableThreadLocal;
    }

    public boolean isPrintRuntimeLog() {
        return this.printRuntimeLog;
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        this.printRuntimeLog = printRuntimeLog;
    }

    public boolean isPrintConfigInfo() {
        return this.printConfigInfo;
    }

    public void setPrintConfigInfo(boolean printConfigInfo) {
        this.printConfigInfo = printConfigInfo;
    }

    public void addConfigPrintExclusion(String fieldName) {
        if (configPrintExclusionList == null)
            this.configPrintExclusionList = new ArrayList<>(1);

        if (!configPrintExclusionList.contains(fieldName))
            this.configPrintExclusionList.add(fieldName);
    }

    public void clearAllConfigPrintExclusion() {
        if (configPrintExclusionList != null) this.configPrintExclusionList.clear();
    }

    public boolean removeConfigPrintExclusion(String fieldName) {
        return configPrintExclusionList != null && configPrintExclusionList.remove(fieldName);
    }

    public boolean existConfigPrintExclusion(String fieldName) {
        return configPrintExclusionList != null && configPrintExclusionList.contains(fieldName);
    }

    //***************************************************************************************************************//
    //                                     3: creation configuration(20)                                             //
    //***************************************************************************************************************//
    public Class<?>[] getObjectInterfaces() {
        return objectInterfaces;
    }

    public void setObjectInterfaces(Class<?>[] interfaces) {
        this.objectInterfaces = interfaces;
    }

    public String[] getObjectInterfaceNames() {
        return this.objectInterfaceNames;
    }

    public void setObjectInterfaceNames(String[] interfaceNames) {
        this.objectInterfaceNames = interfaceNames;
    }

    public BeeObjectFactory<K, V> getObjectFactory() {
        return this.objectFactory;
    }

    public void setObjectFactory(BeeObjectFactory<K, V> factory) {
        this.objectFactory = factory;
    }

    public Class<?> getObjectFactoryClass() {
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

    public BeeObjectPredicate getObjectPredicate() {
        return objectPredicate;
    }

    public void setObjectPredicate(BeeObjectPredicate objectPredicate) {
        this.objectPredicate = objectPredicate;
    }

    public Class<? extends BeeObjectPredicate> getObjectPredicateClass() {
        return objectPredicateClass;
    }

    public void setObjectPredicateClass(Class<? extends BeeObjectPredicate> objectPredicateClass) {
        this.objectPredicateClass = objectPredicateClass;
    }

    public String getObjectPredicateClassName() {
        return objectPredicateClassName;
    }

    public void setObjectPredicateClassName(String objectPredicateClassName) {
        this.objectPredicateClassName = objectPredicateClassName;
    }

    public Object getFactoryProperty(String key) {
        return this.factoryProperties.get(key);
    }

    public Object removeFactoryProperty(String key) {
        return this.factoryProperties.remove(key);
    }

    public void addFactoryProperty(String key, Object value) {
        if (isNotBlank(key) && value != null) this.factoryProperties.put(key, value);
    }

    public void addFactoryProperty(String propertyText) {
        if (isNotBlank(propertyText)) {
            String[] attributeArray = propertyText.split("&");
            for (String attribute : attributeArray) {
                String[] pair = attribute.split("=");
                if (pair.length == 2) {
                    this.factoryProperties.put(pair[0].trim(), pair[1].trim());
                } else {
                    pair = attribute.split(":");
                    if (pair.length == 2) {
                        this.factoryProperties.put(pair[0].trim(), pair[1].trim());
                    }
                }
            }
        }
    }

    //***************************************************************************************************************//
    //                                     4: pool work configuration(2)                                             //
    //***************************************************************************************************************//
    public String getPoolImplementClassName() {
        return this.poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (isNotBlank(poolImplementClassName))
            this.poolImplementClassName = trimString(poolImplementClassName);
    }


    //****************************************************************************************************************//
    //                                    5: Log Collector(18)                                                        //
    //****************************************************************************************************************//
    public int getObjectCallLogCacheSize() {
        return objectCallLogCacheSize;
    }

    public void setObjectCallLogCacheSize(int objectCallLogCacheSize) {
        if (objectCallLogCacheSize <= 0)
            throw new InvalidParameterException("The given value for configuration item 'object-call-log-cache-size' must be greater than zero");
        this.objectCallLogCacheSize = objectCallLogCacheSize;
    }

    public long getSlowObjectGetThreshold() {
        return slowObjectGetThreshold;
    }

    public void setSlowObjectGetThreshold(long slowObjectGetThreshold) {
        if (slowObjectGetThreshold < 0L)
            throw new InvalidParameterException("The given value for configuration item 'slow-object-get-threshold' must be greater than zero");

        this.slowObjectGetThreshold = slowObjectGetThreshold;
    }

    public long getSlowObjectCallThreshold() {
        return slowObjectCallThreshold;
    }

    public void setSlowObjectCallThreshold(long slowObjectCallThreshold) {
        if (slowObjectCallThreshold < 0L)
            throw new InvalidParameterException("The given value for configuration item 'slow-object-call-threshold' must be greater than zero");

        this.slowObjectCallThreshold = slowObjectCallThreshold;
    }

    public boolean isObjectCallLogListenInSync() {
        return objectCallLogListenInSync;
    }

    public void setObjectCallLogListenInSync(boolean objectCallLogListenInSync) {
        this.objectCallLogListenInSync = objectCallLogListenInSync;
    }

    public long getObjectCallLogTimeout() {
        return objectCallLogTimeout;
    }

    public void setObjectCallLogTimeout(long objectCallLogTimeout) {
        if (objectCallLogTimeout <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'object-call-log-timeout' must be greater than zero");
        this.objectCallLogTimeout = objectCallLogTimeout;
    }

    public long getObjectCallLogClearInterval() {
        return objectCallLogClearInterval;
    }

    public void setObjectCallLogClearInterval(long objectCallLogClearInterval) {
        if (objectCallLogClearInterval <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'object-call-log-clear-interval' must be greater than zero");
        this.objectCallLogClearInterval = objectCallLogClearInterval;
    }

    public BeeObjectCallLogListener<K, V> getObjectCallLogListener() {
        return objectCallLogListener;
    }

    public void setObjectCallLogListener(BeeObjectCallLogListener<K, V> objectCallLogListener) {
        this.objectCallLogListener = objectCallLogListener;
    }

    public Class<? extends BeeObjectCallLogListener<K, V>> getObjectCallLogListenerClass() {
        return objectCallLogListenerClass;
    }

    public void setObjectCallLogListenerClass(Class<? extends BeeObjectCallLogListener<K, V>> objectCallLogListenerClass) {
        this.objectCallLogListenerClass = objectCallLogListenerClass;
    }

    public String getObjectCallLogListenerClassName() {
        return objectCallLogListenerClassName;
    }

    public void setObjectCallLogListenerClassName(String objectCallLogListenerClassName) {
        this.objectCallLogListenerClassName = objectCallLogListenerClassName;
    }

    public Class<? extends BeeObjectCallLogCollector<K, V>> getObjectCallLogCollectorClass() {
        return objectCallLogCollectorClass;
    }

    public void setObjectCallLogCollectorClass(Class<? extends BeeObjectCallLogCollector<K, V>> objectCallLogCollectorClass) {
        this.objectCallLogCollectorClass = objectCallLogCollectorClass;
    }

    public BeeObjectCallLogCollector<K, V> getObjectCallLogCollector() {
        return objectCallLogCollector;
    }

    public void setObjectCallLogCollector(BeeObjectCallLogCollector<K, V> objectCallLogCollector) {
        this.objectCallLogCollector = objectCallLogCollector;
    }

    public String getObjectCallLogCollectorClassName() {
        return objectCallLogCollectorClassName;
    }

    public void setObjectCallLogCollectorClassName(String objectCallLogCollectorClassName) {
        this.objectCallLogCollectorClassName = objectCallLogCollectorClassName;
    }

    //***************************************************************************************************************//
    //                                     6: configuration file load(3)                                             //
    //***************************************************************************************************************//
    public void loadFromPropertiesFile(String filename) {
        loadFromPropertiesFile(filename, null);
    }

    public void loadFromPropertiesFile(File file) {
        loadFromPropertiesFile(file, null);
    }

    public void loadFromProperties(Properties configProperties) {
        loadFromProperties(configProperties, null);
    }

    public void loadFromPropertiesFile(String filename, String keyPrefix) {
        if (isBlank(filename))
            throw new IllegalArgumentException("Configuration file name can't be null or empty");
        String fileLowerCaseName = filename.toLowerCase(Locale.US);
        if (!fileLowerCaseName.endsWith(".properties"))
            throw new IllegalArgumentException("Configuration file name file must be end with '.properties'");

        if (fileLowerCaseName.startsWith("cp:")) {//1:'cp:' prefix
            String cpFileName = fileLowerCaseName.substring("cp:".length());
            Properties fileProperties = loadPropertiesFromClassPathFile(cpFileName);
            loadFromProperties(fileProperties, keyPrefix);
        } else if (fileLowerCaseName.startsWith("classpath:")) {//2:'classpath:' prefix
            String cpFileName = fileLowerCaseName.substring("classpath:".length());
            Properties fileProperties = loadPropertiesFromClassPathFile(cpFileName);
            loadFromProperties(fileProperties, keyPrefix);
        } else {//load a real path
            File file = new File(filename);
            if (!file.exists()) throw new IllegalArgumentException("Not found configuration file:" + filename);
            if (!file.isFile())
                throw new IllegalArgumentException("Target object is a valid configuration file," + filename);
            loadFromPropertiesFile(file, keyPrefix);
        }
    }

    public void loadFromPropertiesFile(File file, String keyPrefix) {
        if (file == null) throw new IllegalArgumentException("Configuration properties file can't be null");
        if (!file.exists()) throw new IllegalArgumentException("Configuration properties file not found:" + file);
        if (!file.isFile()) throw new IllegalArgumentException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IllegalArgumentException("Target file is not a properties file");

        try (InputStream stream = Files.newInputStream(file.toPath())) {
            Properties configProperties = new Properties();
            configProperties.load(stream);
            this.loadFromProperties(configProperties, keyPrefix);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load configuration file:" + file, e);
        }
    }

    public void loadFromProperties(Properties configProperties, String keyPrefix) {
        if (configProperties == null || configProperties.isEmpty())
            throw new IllegalArgumentException("Configuration properties can't be null or empty");

        //1: load configuration item values from outside properties
        HashMap<String, String> setValueMap;
        if (isNotBlank(keyPrefix)) {
            if (keyPrefix.charAt(keyPrefix.length() - 1) != '.') keyPrefix = keyPrefix + ".";
            final int keyPrefixLen = keyPrefix.length();
            setValueMap = new HashMap<>(configProperties.size());
            for (Map.Entry<Object, Object> entry : configProperties.entrySet()) {
                String key = (String) entry.getKey();
                if (key.startsWith(keyPrefix)) {
                    setValueMap.put(key.substring(keyPrefixLen), (String) entry.getValue());
                }
            }
        } else {
            setValueMap = new HashMap(configProperties);
        }

        //2: remove some special keys in setValueMap
        String factoryPropertiesText = setValueMap.remove(CONFIG_FACTORY_PROP);
        String factoryPropertiesSizeText = setValueMap.remove(CONFIG_FACTORY_PROP_SIZE);
        String objectInterfacesText = setValueMap.remove(CONFIG_OBJECT_INTERFACES);
        String objectInterfaceNamesText = setValueMap.remove(CONFIG_OBJECT_INTERFACE_NAMES);
        String exclusionListText = setValueMap.remove(CONFIG_CONFIG_PRINT_EXCLUSION_LIST);

        //3:inject item value from map to this dataSource config object
        try {
            setPropertiesValue(this, setValueMap);
        } catch (BeanException e) {
            throw new BeeObjectSourceConfigException(e.getMessage(), e);
        }

        //4:try to find 'factoryProperties' config value
        this.addFactoryProperty(factoryPropertiesText);
        if (isNotBlank(factoryPropertiesSizeText)) {
            int size = Integer.parseInt(factoryPropertiesSizeText.trim());
            for (int i = 1; i <= size; i++)//properties index begin with 1
                this.addFactoryProperty(getPropertyValue(setValueMap, CONFIG_FACTORY_PROP_KEY_PREFIX + i));
        }

        //5:try to find 'objectInterfaceNames' config value
        if (isNotBlank(objectInterfaceNamesText))
            this.objectInterfaceNames = objectInterfaceNamesText.split(",");

        //6:try to find 'objectInterfaces' config value
        if (isNotBlank(objectInterfacesText)) {
            String[] objectInterfaceNameArray = objectInterfacesText.split(",");
            Class<?>[] objectInterfaces = new Class[objectInterfaceNameArray.length];
            for (int i = 0, l = objectInterfaceNameArray.length; i < l; i++) {
                try {
                    objectInterfaces[i] = loadClass(objectInterfaceNameArray[i]);
                } catch (ClassNotFoundException e) {
                    throw new BeeObjectSourceConfigException("Class not found:" + objectInterfaceNameArray[i]);
                }
            }
            this.objectInterfaces = objectInterfaces;
        }

        //7:try to load exclusion list on config print
        if (isNotBlank(exclusionListText)) {
            this.clearAllConfigPrintExclusion();//remove existed exclusion
            for (String exclusion : exclusionListText.trim().split(",")) {
                this.addConfigPrintExclusion(exclusion);
            }
        }
    }

    //***************************************************************************************************************//
    //                                     7: configuration check and object factory create methods(4)               //
    //***************************************************************************************************************//
    //check pool configuration
    public BeeObjectSourceConfig<K, V> check() {
        if (initialSize > this.maxActive)
            throw new BeeObjectSourceConfigException("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'");

        //1: try to create object factory
        BeeObjectFactory<K, V> objectFactory = this.createObjectFactory();
        if (objectFactory.getDefaultKey() == null)
            throw new BeeObjectSourceConfigException("Object factory must provide a non null default pooled key");

        //2: try to load interfaces
        Class<?>[] objectInterfaces = this.loadObjectInterfaces();
        if (objectInterfaces != null) {
            int superClassCount = 0;
            for (Class<?> clazz : objectInterfaces) {
                if (!clazz.isInterface()) {
                    superClassCount++;
                    if (Modifier.isFinal(clazz.getModifiers()))
                        throw new BeeObjectSourceConfigException("Object supper class cannot be final type,class:" + clazz.getName());
                    try {
                        clazz.getDeclaredConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new BeeObjectSourceConfigException("Not found a constructor without parameters in super class:" + clazz.getName());
                    }
                }
            }
            if (superClassCount > 1)
                throw new BeeObjectSourceConfigException("The count of super class cannot be greater than 1");
        }

        //3: create predicate and filter
        BeeObjectPredicate predicate = this.createObjectPredicate();
        //4: create a log collector
        BeeObjectCallLogCollector<K, V> logCollector = this.createLogCollector();
        BeeObjectCallLogListener<K, V> objectCallLogListener = (logCollector != null) ? this.createLogListener() : null;
        //5: create a copy from this current configuration object
        BeeObjectSourceConfig<K, V> checkedConfig = new BeeObjectSourceConfig<>();
        copyTo(checkedConfig);

        //6: assign above objects to the checked configuration object(such as factory,filter,predicate)
        checkedConfig.objectFactory = objectFactory;
        if (predicate != null) checkedConfig.objectPredicate = predicate;
        if (objectInterfaces != null) checkedConfig.objectInterfaces = objectInterfaces;
        if (logCollector != null) checkedConfig.objectCallLogCollector = logCollector;
        if (objectCallLogListener != null) checkedConfig.objectCallLogListener = objectCallLogListener;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "KeyPool-" + PoolNameIndex.getAndIncrement();
        if (checkedConfig.printConfigInfo) printConfiguration(checkedConfig);
        return checkedConfig;
    }

    void copyTo(BeeObjectSourceConfig<K, V> config) {
        //1:copy primitive type fields
        String fieldName = "";
        try {
            for (Field field : BeeObjectSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                fieldName = field.getName();
                switch (fieldName) {
                    case CONFIG_OBJECT_INTERFACES:
                        if (objectInterfaces != null && objectInterfaces.length > 0)
                            config.objectInterfaces = objectInterfaces.clone();
                        break;
                    case CONFIG_OBJECT_INTERFACE_NAMES:
                        if (objectInterfaceNames != null && objectInterfaceNames.length > 0)
                            config.objectInterfaceNames = objectInterfaceNames.clone();
                        break;
                    case CONFIG_FACTORY_PROP:
                        config.factoryProperties.putAll(factoryProperties);
                        break;
                    case CONFIG_CONFIG_PRINT_EXCLUSION_LIST:
                        if (configPrintExclusionList != null && !configPrintExclusionList.isEmpty())
                            config.configPrintExclusionList = new ArrayList<>(configPrintExclusionList);//support empty list copy
                        break;
                    default: //other config items
                        field.set(config, field.get(this));
                }
            }
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to filled value on field[" + fieldName + "]", e);
        }
    }

    private Class<?>[] loadObjectInterfaces() throws BeeObjectSourceConfigException {
        //1: if objectInterfaces field value is not null,then check it and return it
        if (objectInterfaces != null && objectInterfaces.length > 0) {
            for (int i = 0, l = objectInterfaces.length; i < l; i++) {
                if (objectInterfaces[i] == null)
                    throw new BeeObjectSourceConfigException("Object interfaces[" + i + "]is null");
            }
            return objectInterfaces.clone();
        }

        //2: try to load interfaces by names
        final int objectInterfaceNameSize = this.objectInterfaceNames != null ? objectInterfaceNames.length : 0;
        if (objectInterfaceNameSize > 0) {
            Class<?>[] objectInterfaces = new Class[objectInterfaceNameSize];
            for (int i = 0; i < objectInterfaceNameSize; i++) {
                try {
                    if (isBlank(this.objectInterfaceNames[i]))
                        throw new BeeObjectSourceConfigException("Object interface class names[" + i + "]is empty or null");
                    objectInterfaces[i] = loadClass(this.objectInterfaceNames[i]);
                } catch (ClassNotFoundException e) {
                    throw new BeeObjectSourceConfigException("Not found interface class with class names[" + i + "]", e);
                }
            }
            return objectInterfaces;
        }
        return null;
    }

    private BeeObjectFactory<K, V> createObjectFactory() {
        //1: copy from member field of configuration
        BeeObjectFactory<K, V> rawObjectFactory = this.objectFactory;

        //2: create factory instance
        if (rawObjectFactory == null && (objectFactoryClass != null || objectFactoryClassName != null)) {
            Class<?> factoryClass = null;
            try {
                factoryClass = objectFactoryClass != null ? objectFactoryClass : loadClass(objectFactoryClassName);
                rawObjectFactory = (BeeObjectFactory<K, V>) createClassInstance(factoryClass, BeeObjectFactory.class, "object factory");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object factory class:" + objectFactoryClassName, e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object factory by class:" + factoryClass, e);
            }
        }

        //3: throw check failure exception
        if (rawObjectFactory == null)
            throw new BeeObjectSourceConfigException("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]");

        //4: inject properties to factory
        if (!factoryProperties.isEmpty())
            try {
                setPropertiesValue(rawObjectFactory, factoryProperties);
            } catch (BeanException e) {
                throw new BeeObjectSourceConfigException(e.getMessage(), e);
            }

        return rawObjectFactory;
    }

    private BeeObjectPredicate createObjectPredicate() throws BeeObjectSourceConfigException {
        //step1:if exits a set predicate,then return it
        if (this.objectPredicate != null) return this.objectPredicate;

        //step2: create predicate instance with a class or class name
        if (objectPredicateClass != null || isNotBlank(objectPredicateClassName)) {
            Class<?> predicationClass = null;
            try {
                predicationClass = objectPredicateClass != null ? objectPredicateClass : loadClass(objectPredicateClassName);
                return (BeeObjectPredicate) createClassInstance(predicationClass, BeeObjectPredicate.class, "object predicate");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found predicate class:" + objectPredicateClassName, e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create predicate instance with class:" + predicationClass, e);
            }
        }
        return null;
    }

    //create object call log listener
    private BeeObjectCallLogListener<K, V> createLogListener() {
        //step1:if exists listener,then return it
        if (this.objectCallLogListener != null) return this.objectCallLogListener;

        //step2: create a listener
        if (this.objectCallLogListenerClass != null || isNotBlank(this.objectCallLogListenerClassName)) {
            Class<?> listenerClass = null;
            try {
                listenerClass = objectCallLogListenerClass != null ? objectCallLogListenerClass : loadClass(objectCallLogListenerClassName);
                return (BeeObjectCallLogListener<K, V>) createClassInstance(listenerClass, BeeObjectCallLogListener.class, "object call log listener");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create object call log listener with class[" + objectCallLogListenerClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create object call log listener with class[" + listenerClass + "]", e);
            }
        }
        return null;
    }

    //create object call log collector
    private BeeObjectCallLogCollector<K, V> createLogCollector() {
        //step1:if exists log collector,then return it
        if (this.objectCallLogCollector != null) return this.objectCallLogCollector;

        //step2: create object method log collector
        if (this.objectCallLogCollectorClass != null || isNotBlank(this.objectCallLogCollectorClassName)) {
            Class<?> collectorClass = null;
            try {
                collectorClass = objectCallLogCollectorClass != null ? objectCallLogCollectorClass : loadClass(objectCallLogCollectorClassName);
                return (BeeObjectCallLogCollector<K, V>) createClassInstance(collectorClass, BeeObjectCallLogCollector.class, "object call log collector");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create object call log collector with class[" + objectCallLogCollectorClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create object call log collector with class[" + collectorClass + "]", e);
            }
        }
        return null;
    }

    //print check passed configuration
    private void printConfiguration(BeeObjectSourceConfig<K, V> checkedConfig) {
        String poolName = checkedConfig.poolName;
        List<String> exclusionList = checkedConfig.configPrintExclusionList;
        CommonLog.info("................................................BeeOP({})configuration[start]................................................", poolName);

        try {
            for (Field field : BeeObjectSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                String fieldName = field.getName();
                boolean infoPrint = exclusionList == null || !exclusionList.contains(fieldName);

                switch (fieldName) {
                    case CONFIG_OBJECT_INTERFACES: {
                        if (objectInterfaces != null && objectInterfaces.length > 0) {
                            StringBuilder interfacesClassBuf = new StringBuilder(20);
                            for (Class<?> clazz : objectInterfaces) {
                                if (!interfacesClassBuf.isEmpty()) interfacesClassBuf.append(",");
                                interfacesClassBuf.append(clazz);
                            }
                            if (infoPrint)
                                CommonLog.info("BeeOP({}).objectInterfaces=[{}]", poolName, interfacesClassBuf);
                            else
                                CommonLog.debug("BeeOP({}).objectInterfaces=[{}]", poolName, interfacesClassBuf);
                        }
                        break;
                    }
                    case CONFIG_OBJECT_INTERFACE_NAMES: {
                        if (objectInterfaceNames != null && objectInterfaceNames.length > 0) {
                            StringBuilder interfaceNameBuf = new StringBuilder(20);
                            for (String name : objectInterfaceNames) {
                                if (!interfaceNameBuf.isEmpty()) interfaceNameBuf.append(",");
                                interfaceNameBuf.append(name);
                            }
                            if (infoPrint)
                                CommonLog.info("BeeOP({}).objectInterfaceNames=[{}]", poolName, interfaceNameBuf);
                            else
                                CommonLog.debug("BeeOP({}).objectInterfaceNames=[{}]", poolName, interfaceNameBuf);
                        }
                        break;
                    }
                    case CONFIG_FACTORY_PROP: {
                        if (!this.factoryProperties.isEmpty()) {
                            if (infoPrint) {
                                for (Map.Entry<String, Object> entry : checkedConfig.factoryProperties.entrySet())
                                    CommonLog.info("BeeCP({}).factoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            } else {
                                for (Map.Entry<String, Object> entry : checkedConfig.factoryProperties.entrySet())
                                    CommonLog.debug("BeeCP({}).factoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            }
                        }
                        break;
                    }
                    case CONFIG_CONFIG_PRINT_EXCLUSION_LIST:
                        break;
                    default:
                        if (infoPrint)
                            CommonLog.info("BeeOP({}).{}={}", poolName, fieldName, field.get(checkedConfig));
                        else
                            CommonLog.debug("BeeOP({}).{}={}", poolName, fieldName, field.get(checkedConfig));
                }
            }
        } catch (Throwable e) {
            CommonLog.warn("BeeOP({})failed to print configuration", poolName, e);
        }
        CommonLog.info("................................................BeeOP({})configuration[end]................................................", poolName);
    }
}

