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

import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.BeanUtil.*;
import static org.stone.tools.CommonUtil.*;

/**
 * Bee object source configuration object,which is not thread-safe.
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

    //1: Pool name,default is none; if not set,a name generated with {@code PoolNameIndex} for it
    private String poolName;
    //2: Object getting mode in pool
    private boolean fairMode;
    //3: Object creation size during pool initialization,default is zero
    private int initialSize;
    //4: Max reachable size of object categories in pool,default is 50
    private int maxKeySize = 50;
    //5: Max reachable size of pooled objects of per category,pool total capacity = maxObjectKeySize * maxActive
    private int maxActive = Math.min(Math.max(10, CommonUtil.NCPU), 50);
    //6: Permit size of semaphore for per object category
    private int semaphoreSize = Math.min(this.maxActive / 2, CommonUtil.NCPU);
    //7: A flag,true is that pool use a threadLocal to store used object for borrowers(false can be used to support virtual threads)
    private boolean useThreadLocal = true;
    //8: Milliseconds,max wait time for a borrower to get a object from pool,default is 8000 milliseconds(8 seconds)
    private long maxWait = 8000L;
    //9: A flag of object creation,true that pool use a thread to create initial objects during initialization,default is false
    private boolean asyncCreateInitObject;
    //10: Milliseconds,max idle time of pooled objects stay in pool,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = 180000L;
    //11: Milliseconds: max inactive time of borrowed objects,which are recycled when timeout;default is zero,this parameter disabled
    private long holdTimeout;
    //12: Milliseconds: an interval time of pool thread to find out timeout objects(idle timeout and hold timeout),default is 18000 milliseconds(3 minutes)
    private long intervalToClearTimeout = 180000L;
    //15: Seconds,max wait time to get alive test result on borrowed objects,default is 3 seconds.
    private int aliveTestTimeout = 3;
    //16: Milliseconds,a threshold time of alive since from last test,if gap time is less than it,assume objects are alive,and skip test,default is 500 milliseconds
    private long aliveAssumeTime = 500L;
    //17: A flag that how to close borrowed objects when pool close or pool clean,true is that pool recycles them immediately,false that pool wait them return to pool,default is false.
    private boolean forceRecycleBorrowedOnClose;
    //18: A flag that shutdown thread pool when restart or shutdown object pool.
    private boolean forceShutdownThreadPoolOnClose;
    //19: Milliseconds,wait time for pool to wait borrowed objects return to pool during pool close or pool clear,default is 3000 milliseconds
    private long parkTimeForRetry = 3000L;
    //20: A flag to enable Jmx registration,default is false
    private boolean registerMbeans;
    //21: A flag to enable runtime log print in pool,default is false
    private boolean printRuntimeLogs;
    //22: A flag to enable configuration log print during pool initializes,default is false
    private boolean printConfiguration;
    //23: An exclusion list of configuration print,default is null
    private List<String> exclusionListOfPrint;
    //24: Class name of pool implementation,default is {@code KeyedObjectPool}
    private String poolImplementClassName = KeyedObjectPool.class.getName();


    //25: An array of interfaces implemented by object class
    private Class<?>[] objectInterfaces;
    //26: A class name array of interface implemented by object class
    private String[] objectInterfaceNames;

    //27: Object factory,priority order: instance > class > class name
    private BeeObjectFactory<K, V> objectFactory;
    //28: Class of object factory
    private Class<? extends BeeObjectFactory<K, V>> objectFactoryClass;
    //29: Class name of object factory
    private String objectFactoryClassName;

    //30: Predicate to do eviction test on exception objects,priority order: instance > class > class name
    private BeeObjectPredicate predicate;
    //31: Class of predicate
    private Class<? extends BeeObjectPredicate> predicateClass;
    //32: Class name of predicate
    private String predicateClassName;


    //********************************************** object event logs **************************************************//
    //33: Capacity of method logs cache，default is 1000
    private int eventLogCacheSize = 1000;
    //34: Log timeout in manager,default is 3 minutes
    private long eventLogTimeout = 180000L;
    //35: Timer interval to clear timeout logs,default is 3 minutes
    private long intervalToClearTimeoutEventLogs = eventLogTimeout;

    //36: Object call logs manager,priority order: instance > class > class name
    private BeeObjectEventLogManager<K, V> eventLogManager;
    //37: Class of object call logs manager,default is none
    private Class<? extends BeeObjectEventLogManager<K, V>> eventLogManagerClass;
    //38: Class name of object call logs manager,default is none
    private String eventLogManagerClassName;

    //39: Slow logs handle mode,default is true
    private boolean eventLogHandledBySyncMode = true;
    //40: Slow threshold value of object get,default is 30 seconds,time unit:milliseconds
    private long slowObjectGetThreshold = 30000L;
    //41: Slow threshold of object call,default is 30 seconds,time unit:milliseconds
    private long slowObjectCallThreshold = 30000L;

    //42: Object call log handler,priority order: instance > class > class name
    private BeeObjectEventLogHandler<K, V> eventLogHandler;
    //43: Class of object call log handler,default is none
    private Class<? extends BeeObjectEventLogHandler<K, V>> eventLogHandlerClass;
    //44: Class name of log handler,default is none
    private String eventLogHandlerClassName;

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
    //                                     2:  Pool control setting(46)[1 --- 24]                                       //
    //***************************************************************************************************************//
    @Override
    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

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
            throw new InvalidParameterException("The given value for the configuration item 'initial-size' cannot be less than zero");
        this.initialSize = initialSize;
    }

    public int getMaxKeySize() {
        return maxKeySize;
    }

    public void setMaxKeySize(int maxKeySize) {
        if (maxKeySize <= 0)
            throw new InvalidParameterException("The given value for configuration item 'max-key-size' must be greater than zero");
        this.maxKeySize = maxKeySize;
    }

    @Override
    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (maxActive <= 0)
            throw new InvalidParameterException("The given value for configuration item 'max-active' must be greater than zero");
        this.maxActive = maxActive;
        this.semaphoreSize = (maxActive > 1) ? Math.min(maxActive / 2, CommonUtil.NCPU) : 1;
    }

    @Override
    public int getSemaphoreSize() {
        return semaphoreSize;
    }

    public void setSemaphoreSize(int semaphoreSize) {
        if (semaphoreSize <= 0)
            throw new InvalidParameterException("The given value for configuration item 'borrow-semaphore-size' must be greater than zero");
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
            throw new InvalidParameterException("The given value for configuration item 'max-wait' must be greater than zero");
        this.maxWait = maxWait;
    }

    public boolean isAsyncCreateInitObject() {
        return asyncCreateInitObject;
    }

    public void setAsyncCreateInitObject(boolean asyncCreateInitObject) {
        this.asyncCreateInitObject = asyncCreateInitObject;
    }

    @Override
    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'idle-timeout' must be greater than zero");
        this.idleTimeout = idleTimeout;
    }

    @Override
    public long getHoldTimeout() {
        return holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout < 0L)
            throw new InvalidParameterException("The given value for configuration item 'hold-timeout' cannot be less than zero");
        this.holdTimeout = holdTimeout;
    }

    @Override
    public long getIntervalToClearTimeout() {
        return intervalToClearTimeout;
    }

    public void setIntervalToClearTimeout(long intervalToClearTimeout) {
        if (intervalToClearTimeout <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'timer-check-interval' must be greater than zero");
        this.intervalToClearTimeout = intervalToClearTimeout;
    }

    @Override
    public int getAliveTestTimeout() {
        return aliveTestTimeout;
    }

    public void setAliveTestTimeout(int aliveTestTimeout) {
        if (aliveTestTimeout < 0L)
            throw new InvalidParameterException("The given value for configuration item 'alive-test-timeout' cannot  be less than zero");
        this.aliveTestTimeout = aliveTestTimeout;
    }

    @Override
    public long getAliveAssumeTime() {
        return aliveAssumeTime;
    }

    public void setAliveAssumeTime(long aliveAssumeTime) {
        if (aliveAssumeTime < 0L)
            throw new InvalidParameterException("The given value for configuration item 'alive-assume-time' cannot be less than zero");
        this.aliveAssumeTime = aliveAssumeTime;
    }

    @Override
    public boolean isForceRecycleBorrowedOnClose() {
        return forceRecycleBorrowedOnClose;
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

    @Override
    public long getParkTimeForRetry() {
        return parkTimeForRetry;
    }

    public void setParkTimeForRetry(long parkTimeForRetry) {
        if (parkTimeForRetry < 0L)
            throw new InvalidParameterException("The given value for configuration item 'park-time-for-retry' cannot be less than zero");
        this.parkTimeForRetry = parkTimeForRetry;
    }

    @Override
    public boolean isRegisterMbeans() {
        return registerMbeans;
    }

    public void setRegisterMbeans(boolean registerMbeans) {
        this.registerMbeans = registerMbeans;
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

    public void clearExclusionListOfPrint() {
        if (exclusionListOfPrint != null) this.exclusionListOfPrint.clear();
    }

    public boolean removeExclusionNameOfPrint(String fieldName) {
        return exclusionListOfPrint != null && exclusionListOfPrint.remove(fieldName);
    }

    public boolean existExclusionNameOfPrint(String fieldName) {
        return exclusionListOfPrint != null && exclusionListOfPrint.contains(fieldName);
    }

    @Override
    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        this.poolImplementClassName = poolImplementClassName;
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


    //****************************************************************************************************************//
    //                                    5: Log manager(18)                                                        //
    //****************************************************************************************************************//
    public BeeObjectEventLogManager<K, V> getEventLogManager() {
        return eventLogManager;
    }

    public void setEventLogManager(BeeObjectEventLogManager<K, V> eventLogManager) {
        this.eventLogManager = eventLogManager;
    }

    public Class<? extends BeeObjectEventLogManager<K, V>> getEventLogManagerClass() {
        return eventLogManagerClass;
    }

    public void setEventLogManagerClass(Class<? extends BeeObjectEventLogManager<K, V>> eventLogManagerClass) {
        this.eventLogManagerClass = eventLogManagerClass;
    }

    public String getEventLogManagerClassName() {
        return eventLogManagerClassName;
    }

    public void setEventLogManagerClassName(String eventLogManagerClassName) {
        this.eventLogManagerClassName = eventLogManagerClassName;
    }

    public int getEventLogCacheSize() {
        return eventLogCacheSize;
    }

    public void setEventLogCacheSize(int eventLogCacheSize) {
        if (eventLogCacheSize <= 0)
            throw new InvalidParameterException("The given value for configuration item 'event-log-cache-size' must be greater than zero");
        this.eventLogCacheSize = eventLogCacheSize;
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


    public long getEventLogTimeout() {
        return eventLogTimeout;
    }

    public void setEventLogTimeout(long eventLogTimeout) {
        if (eventLogTimeout <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'event-log-timeout' must be greater than zero");
        this.eventLogTimeout = eventLogTimeout;
    }

    public long getIntervalToClearTimeoutEventLogs() {
        return intervalToClearTimeoutEventLogs;
    }

    public void setIntervalToClearTimeoutEventLogs(long intervalToClearTimeoutEventLogs) {
        if (intervalToClearTimeoutEventLogs <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'interval-to-clear-timeout-event-logs' must be greater than zero");
        this.intervalToClearTimeoutEventLogs = intervalToClearTimeoutEventLogs;
    }

    public BeeObjectEventLogHandler<K, V> getEventLogHandler() {
        return eventLogHandler;
    }

    public void setEventLogHandler(BeeObjectEventLogHandler<K, V> eventLogHandler) {
        this.eventLogHandler = eventLogHandler;
    }

    public Class<? extends BeeObjectEventLogHandler<K, V>> getEventLogHandlerClass() {
        return eventLogHandlerClass;
    }

    public void setEventLogHandlerClass(Class<? extends BeeObjectEventLogHandler<K, V>> eventLogHandlerClass) {
        this.eventLogHandlerClass = eventLogHandlerClass;
    }

    public String getEventLogHandlerClassName() {
        return eventLogHandlerClassName;
    }

    public void setEventLogHandlerClassName(String eventLogHandlerClassName) {
        this.eventLogHandlerClassName = eventLogHandlerClassName;
    }


    public boolean isEventLogHandledBySyncMode() {
        return eventLogHandledBySyncMode;
    }

    public void setEventLogHandledBySyncMode(boolean eventLogHandledBySyncMode) {
        this.eventLogHandledBySyncMode = eventLogHandledBySyncMode;
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
        String exclusionListText = setValueMap.remove(CONFIG_EXCLUSION_LIST_OF_PRINT);

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
            this.clearExclusionListOfPrint();//remove existed exclusion
            for (String exclusion : exclusionListText.trim().split(",")) {
                this.addExclusionNameOfPrint(exclusion);
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
        //4: create a log manager
        BeeObjectEventLogManager<K, V> logManager = this.createLogManager();
        BeeObjectEventLogHandler<K, V> logHandler = (logManager != null) ? this.createLogHandler() : null;
        //5: create a copy from this current configuration object
        BeeObjectSourceConfig<K, V> checkedConfig = new BeeObjectSourceConfig<>();
        copyTo(checkedConfig);

        //6: assign above objects to the checked configuration object(such as factory,filter,predicate)
        checkedConfig.objectFactory = objectFactory;
        if (predicate != null) checkedConfig.predicate = predicate;
        if (objectInterfaces != null) checkedConfig.objectInterfaces = objectInterfaces;
        if (logManager != null) checkedConfig.eventLogManager = logManager;
        if (logHandler != null) checkedConfig.eventLogHandler = logHandler;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "KeyPool-" + PoolNameIndex.getAndIncrement();
        if (checkedConfig.printConfiguration) printConfiguration(checkedConfig);
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
                    case CONFIG_EXCLUSION_LIST_OF_PRINT:
                        if (exclusionListOfPrint != null && !exclusionListOfPrint.isEmpty())
                            config.exclusionListOfPrint = new ArrayList<>(exclusionListOfPrint);//support empty list copy
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
        if (this.predicate != null) return this.predicate;

        //step2: create predicate instance with a class or class name
        if (predicateClass != null || isNotBlank(predicateClassName)) {
            Class<?> predicationClass = null;
            try {
                predicationClass = predicateClass != null ? predicateClass : loadClass(predicateClassName);
                return (BeeObjectPredicate) createClassInstance(predicationClass, BeeObjectPredicate.class, "object predicate");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found predicate class:" + predicateClassName, e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create predicate instance with class:" + predicationClass, e);
            }
        }
        return null;
    }

    //create object call log handler
    private BeeObjectEventLogHandler<K, V> createLogHandler() {
        //step1:if exists handler,then return it
        if (this.eventLogHandler != null) return this.eventLogHandler;

        //step2: create a handler
        if (this.eventLogHandlerClass != null || isNotBlank(this.eventLogHandlerClassName)) {
            Class<?> handlerClass = null;
            try {
                handlerClass = eventLogHandlerClass != null ? eventLogHandlerClass : loadClass(eventLogHandlerClassName);
                return (BeeObjectEventLogHandler<K, V>) createClassInstance(handlerClass, BeeObjectEventLogHandler.class, "object call log handler");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create object call log handler with class[" + eventLogHandlerClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create object call log handler with class[" + handlerClass + "]", e);
            }
        }
        return null;
    }

    //create object call log manager
    private BeeObjectEventLogManager<K, V> createLogManager() {
        //step1:if exists log manager,then return it
        if (this.eventLogManager != null) return this.eventLogManager;

        //step2: create object method log manager
        if (this.eventLogManagerClass != null || isNotBlank(this.eventLogManagerClassName)) {
            Class<?> managerClass = null;
            try {
                managerClass = eventLogManagerClass != null ? eventLogManagerClass : loadClass(eventLogManagerClassName);
                return (BeeObjectEventLogManager<K, V>) createClassInstance(managerClass, BeeObjectEventLogManager.class, "object call log manager");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create object call log manager with class[" + eventLogManagerClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create object call log manager with class[" + managerClass + "]", e);
            }
        }
        return null;
    }

    //print check passed configuration
    private void printConfiguration(BeeObjectSourceConfig<K, V> checkedConfig) {
        String poolName = checkedConfig.poolName;
        List<String> exclusionList = checkedConfig.exclusionListOfPrint;
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
                    case CONFIG_EXCLUSION_LIST_OF_PRINT:
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

