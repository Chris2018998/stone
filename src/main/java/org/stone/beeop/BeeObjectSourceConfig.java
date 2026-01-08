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
import java.lang.reflect.Modifier;
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
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //A map stores some properties of object factory,these properties injected to factory during pool initialization
    private final Map<String, Object> objectFactoryProperties = new HashMap<>(0);

    //1: Pool name,default is none; if not set,a name generated with {@code PoolNameIndex} for it
    private String poolName;
    //2: Object getting mode in pool
    private boolean fairMode;
    //3: Object creation size during pool initialization,default is zero
    private int initialSize;
    //4: Max reachable size of object categories in pool,default is 10
    private int maxKeySize = 10;
    //5: Max reachable size of pooled objects of per category,pool total capacity = maxObjectKeySize * maxActive
    private int maxActive = Math.min(Math.max(10, NCPU), 50);
    //6: Permit size of semaphore for per object category
    private int semaphoreSize = Math.min(this.maxActive / 2, NCPU);
    //7: A flag,true is that pool use a threadLocal to store used object for borrowers(false can be used to support virtual threads)
    private boolean useThreadLocal = true;
    //8: Milliseconds,max wait time for a borrower to get an object from pool,default is 8000 milliseconds(8 seconds)
    private long maxWait = 8000L;
    //9: A flag of object creation,true that pool use a thread to create initial objects during initialization,default is false
    private boolean asyncCreateInitObjects;
    //10: Milliseconds,max idle time of pooled objects stay in pool,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = 180000L;
    //11: Milliseconds: max inactive time of borrowed objects,which are recycled when timeout;default is zero,this parameter disabled
    private long holdTimeout;
    //12: Milliseconds: an interval time of pool thread to find out timeout objects(idle timeout and hold timeout),default is 18000 milliseconds(3 minutes)
    private long intervalOfClearTimeout = 180000L;
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
    //24: A list of names of methods when them be called(last access time update,exception eviction test,method execution logs)
    private List<String> methodNameListOnListen;

    //25: Class name of pool implementation,default is {@code KeyedObjectPool}
    private String poolImplementClassName;
    //26: An array of interfaces implemented by object class
    private Class<?>[] objectInterfaces;
    //27: A class name array of interface implemented by object class
    private String[] objectInterfaceNames;

    //28: Object factory,priority order: instance > class > class name
    private BeeObjectFactory<K, V> objectFactory;
    //29: Class of object factory
    private Class<? extends BeeObjectFactory<K, V>> objectFactoryClass;
    //30: Class name of object factory
    private String objectFactoryClassName;

    //31: Predicate to do eviction test on exception objects,priority order: instance > class > class name
    private BeeObjectPredicate predicate;
    //32: Class of predicate
    private Class<? extends BeeObjectPredicate> predicateClass;
    //33: Class name of predicate
    private String predicateClassName;

    //********************************************** method Execution logs ********************************************//
    //34: A flag to enable method log cache
    private boolean enableMethodExecutionLogCache;
    //35: Capacity of method logs cache，default is 1000
    private int methodExecutionLogCacheSize = 1000;
    //36: Log timeout in manager,default is 3 minutes
    private long methodExecutionLogTimeout = 180000L;
    //37: Timer interval to clear timeout logs,default is 3 minutes
    private long intervalOfClearTimeoutExecutionLogs = methodExecutionLogTimeout;

    //38: Slow threshold value of object get,default is 30 seconds,time unit:milliseconds
    private long slowObjectGetThreshold = 30000L;
    //39: Slow threshold of object call,default is 30 seconds,time unit:milliseconds
    private long slowObjectExecutionThreshold = 30000L;

    //40: method execution listener: instance > class > class name
    private BeeMethodExecutionListener<K> methodExecutionListener;
    //41: Class of method execution listener,default is none
    private Class<? extends BeeMethodExecutionListener<K>> methodExecutionListenerClass;
    //42: Class name of method execution listener,default is none
    private String methodExecutionListenerClassName;

    //43: method execution listener factory: instance > class > class name
    private BeeMethodExecutionListenerFactory<K> methodExecutionListenerFactory;
    //44: Class of method execution listener factory ,default is none
    private Class<? extends BeeMethodExecutionListenerFactory<K>> methodExecutionListenerFactoryClass;
    //45: Class name of method execution listener factory,default is none
    private String methodExecutionListenerFactoryClassName;

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
    //                                     2:  Pool control setting(46)[1 --- 24]                                     //
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
            throw new BeeObjectSourceConfigException("The given value for the configuration item 'initial-size' cannot be less than zero");
        this.initialSize = initialSize;
    }

    public int getMaxKeySize() {
        return maxKeySize;
    }

    public void setMaxKeySize(int maxKeySize) {
        if (maxKeySize <= 0)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'max-key-size' must be greater than zero");
        this.maxKeySize = maxKeySize;
    }

    @Override
    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (maxActive <= 0)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'max-active' must be greater than zero");
        this.maxActive = maxActive;
        this.semaphoreSize = (maxActive > 1) ? Math.min(maxActive / 2, NCPU) : 1;
    }

    @Override
    public int getSemaphoreSize() {
        return semaphoreSize;
    }

    public void setSemaphoreSize(int semaphoreSize) {
        if (semaphoreSize <= 0)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'borrow-semaphore-size' must be greater than zero");
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
            throw new BeeObjectSourceConfigException("The given value for configuration item 'max-wait' must be greater than zero");
        this.maxWait = maxWait;
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
            throw new BeeObjectSourceConfigException("The given value for configuration item 'idle-timeout' must be greater than zero");
        this.idleTimeout = idleTimeout;
    }

    @Override
    public long getHoldTimeout() {
        return holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout < 0L)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'hold-timeout' cannot be less than zero");
        this.holdTimeout = holdTimeout;
    }

    @Override
    public long getIntervalOfClearTimeout() {
        return intervalOfClearTimeout;
    }

    public void setIntervalOfClearTimeout(long intervalOfClearTimeout) {
        if (intervalOfClearTimeout <= 0L)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'interval-of-clear-timeout' must be greater than zero");
        this.intervalOfClearTimeout = intervalOfClearTimeout;
    }

    @Override
    public int getAliveTestTimeout() {
        return aliveTestTimeout;
    }

    public void setAliveTestTimeout(int aliveTestTimeout) {
        if (aliveTestTimeout < 0L)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'alive-test-timeout' cannot  be less than zero");
        this.aliveTestTimeout = aliveTestTimeout;
    }

    @Override
    public long getAliveAssumeTime() {
        return aliveAssumeTime;
    }

    public void setAliveAssumeTime(long aliveAssumeTime) {
        if (aliveAssumeTime < 0L)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'alive-assume-time' cannot be less than zero");
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
            throw new BeeObjectSourceConfigException("The given value for configuration item 'park-time-for-retry' cannot be less than zero");
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

    public void addListenMethodName(String methodName) {
        if (isBlank(methodName))
            throw new BeeObjectSourceConfigException("The given value for configuration item 'method-name' can't be null or blank");
        if (this.methodNameListOnListen == null) this.methodNameListOnListen = new ArrayList<>(1);
        if (!methodNameListOnListen.contains(methodName)) methodNameListOnListen.add(methodName);
    }

    public void removeListenMethodName(String methodName) {
        if (this.methodNameListOnListen != null) methodNameListOnListen.remove(methodName);
    }

    public List<String> getMethodNameListOnListen() {
        return this.methodNameListOnListen;
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
        return this.objectFactoryProperties.get(key);
    }

    public Object removeObjectFactoryProperty(String key) {
        return this.objectFactoryProperties.remove(key);
    }

    public void addObjectFactoryProperty(String key, Object value) {
        if (isNotBlank(key) && value != null) this.objectFactoryProperties.put(key, value);
    }

    public void addObjectFactoryProperty(String propertyText) {
        if (isNotBlank(propertyText)) {
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
    //                                    5: method execution logs (24)                                               //
    //****************************************************************************************************************//
    public boolean isEnableMethodExecutionLogCache() {
        return enableMethodExecutionLogCache;
    }

    public void setEnableMethodExecutionLogCache(boolean enableMethodExecutionLogCache) {
        this.enableMethodExecutionLogCache = enableMethodExecutionLogCache;
    }

    public int getMethodExecutionLogCacheSize() {
        return methodExecutionLogCacheSize;
    }

    public void setMethodExecutionLogCacheSize(int methodExecutionLogCacheSize) {
        if (methodExecutionLogCacheSize <= 0)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'method-execution-log-cache-size' must be greater than zero");
        this.methodExecutionLogCacheSize = methodExecutionLogCacheSize;
    }

    public long getSlowObjectGetThreshold() {
        return slowObjectGetThreshold;
    }

    public void setSlowObjectGetThreshold(long slowObjectGetThreshold) {
        if (slowObjectGetThreshold < 0L)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'slow-object-get-threshold' must be greater than zero");

        this.slowObjectGetThreshold = slowObjectGetThreshold;
    }

    public long getSlowObjectExecutionThreshold() {
        return slowObjectExecutionThreshold;
    }

    public void setSlowObjectExecutionThreshold(long slowObjectExecutionThreshold) {
        if (slowObjectExecutionThreshold < 0L)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'slow-object-execution-threshold' must be greater than zero");

        this.slowObjectExecutionThreshold = slowObjectExecutionThreshold;
    }

    public long getMethodExecutionLogTimeout() {
        return methodExecutionLogTimeout;
    }

    public void setMethodExecutionLogTimeout(long methodExecutionLogTimeout) {
        if (methodExecutionLogTimeout <= 0L)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'method-execution-log-timeout' must be greater than zero");
        this.methodExecutionLogTimeout = methodExecutionLogTimeout;
    }

    public long getIntervalOfClearTimeoutExecutionLogs() {
        return intervalOfClearTimeoutExecutionLogs;
    }

    public void setIntervalOfClearTimeoutExecutionLogs(long intervalOfClearTimeoutExecutionLogs) {
        if (intervalOfClearTimeoutExecutionLogs <= 0L)
            throw new BeeObjectSourceConfigException("The given value for configuration item 'interval-of-clear-timeout-execution-logs' must be greater than zero");
        this.intervalOfClearTimeoutExecutionLogs = intervalOfClearTimeoutExecutionLogs;
    }

    public BeeMethodExecutionListener<K> getMethodExecutionListener() {
        return methodExecutionListener;
    }

    public void setMethodExecutionListener(BeeMethodExecutionListener<K> methodExecutionListener) {
        this.methodExecutionListener = methodExecutionListener;
    }

    public Class<? extends BeeMethodExecutionListener<K>> getMethodExecutionListenerClass() {
        return methodExecutionListenerClass;
    }

    public void setMethodExecutionListenerClass(Class<? extends BeeMethodExecutionListener<K>> methodExecutionListenerClass) {
        this.methodExecutionListenerClass = methodExecutionListenerClass;
    }

    public String getMethodExecutionListenerClassName() {
        return methodExecutionListenerClassName;
    }

    public void setMethodExecutionListenerClassName(String methodExecutionListenerClassName) {
        this.methodExecutionListenerClassName = methodExecutionListenerClassName;
    }

    public BeeMethodExecutionListenerFactory<K> getMethodExecutionListenerFactory() {
        return methodExecutionListenerFactory;
    }

    public void setMethodExecutionListenerFactory(BeeMethodExecutionListenerFactory<K> methodExecutionListenerFactory) {
        this.methodExecutionListenerFactory = methodExecutionListenerFactory;
    }

    public Class<? extends BeeMethodExecutionListenerFactory<K>> getMethodExecutionListenerFactoryClass() {
        return methodExecutionListenerFactoryClass;
    }

    public void setMethodExecutionListenerFactoryClass(Class<? extends BeeMethodExecutionListenerFactory<K>> methodExecutionListenerFactoryClass) {
        this.methodExecutionListenerFactoryClass = methodExecutionListenerFactoryClass;
    }

    public String getMethodExecutionListenerFactoryClassName() {
        return methodExecutionListenerFactoryClassName;
    }

    public void setMethodExecutionListenerFactoryClassName(String methodExecutionListenerFactoryClassName) {
        this.methodExecutionListenerFactoryClassName = methodExecutionListenerFactoryClassName;
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
            throw new BeeObjectSourceConfigException("Configuration file name can't be null or empty");
        String fileLowerCaseName = filename.toLowerCase(Locale.US);
        if (!fileLowerCaseName.endsWith(".properties"))
            throw new BeeObjectSourceConfigException("Configuration file name file must be end with '.properties'");

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
            if (!file.exists()) throw new BeeObjectSourceConfigException("Not found configuration file:" + filename);
            if (!file.isFile())
                throw new BeeObjectSourceConfigException("Target object is a valid configuration file," + filename);
            loadFromPropertiesFile(file, keyPrefix);
        }
    }

    public void loadFromPropertiesFile(File file, String keyPrefix) {
        if (file == null) throw new BeeObjectSourceConfigException("Configuration properties file can't be null");
        if (!file.exists()) throw new BeeObjectSourceConfigException("Configuration properties file not found:" + file);
        if (!file.isFile()) throw new BeeObjectSourceConfigException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new BeeObjectSourceConfigException("Target file is not a properties file");

        try (InputStream stream = Files.newInputStream(file.toPath())) {
            Properties configProperties = new Properties();
            configProperties.load(stream);
            this.loadFromProperties(configProperties, keyPrefix);
        } catch (IOException e) {
            throw new BeeObjectSourceConfigException("Failed to load configuration file:" + file, e);
        }
    }

    public void loadFromProperties(Properties configProperties, String keyPrefix) {
        if (configProperties == null || configProperties.isEmpty())
            throw new BeeObjectSourceConfigException("Configuration properties can't be null or empty");

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
        this.addObjectFactoryProperty(factoryPropertiesText);
        if (isNotBlank(factoryPropertiesSizeText)) {
            int size = Integer.parseInt(factoryPropertiesSizeText.trim());
            for (int i = 1; i <= size; i++)//properties index begin with 1
                this.addObjectFactoryProperty(getPropertyValue(setValueMap, CONFIG_FACTORY_PROP_KEY_PREFIX + i));
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
        //4: create a method log listener
        BeeMethodExecutionListener<K> methodExecutionListener = this.createMethodExecutionListener();
        //5: create a copy from this current configuration object
        BeeObjectSourceConfig<K, V> checkedConfig = new BeeObjectSourceConfig<>();
        copyTo(checkedConfig);

        //6: assign above objects to the checked configuration object(such as factory,filter,predicate)
        checkedConfig.objectFactory = objectFactory;
        if (predicate != null) checkedConfig.predicate = predicate;
        if (objectInterfaces != null) checkedConfig.objectInterfaces = objectInterfaces;
        if (methodExecutionListener != null) checkedConfig.methodExecutionListener = methodExecutionListener;
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
                        config.objectFactoryProperties.putAll(objectFactoryProperties);
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
            Class<? extends BeeObjectFactory<K, V>> factoryClass = null;
            try {
                factoryClass = objectFactoryClass != null ? objectFactoryClass : loadClass(objectFactoryClassName);
                rawObjectFactory = createClassInstance(factoryClass, BeeObjectFactory.class, "object factory");
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
        if (!objectFactoryProperties.isEmpty())
            try {
                setPropertiesValue(rawObjectFactory, objectFactoryProperties);
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
    private BeeMethodExecutionListener<K> createMethodExecutionListener() {
        //step1:if exists handler,then return it
        if (this.methodExecutionListener != null) return this.methodExecutionListener;

        //step2:if exists listener factory,then use it to create one
        if (this.methodExecutionListenerFactory != null) {
            try {
                return methodExecutionListenerFactory.create(this);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create method execution listener by listener factory", e);
            }
        }

        //step3: create listener factory and let it create a listener
        if (this.methodExecutionListenerFactoryClass != null || isNotBlank(this.methodExecutionListenerFactoryClassName)) {
            BeeMethodExecutionListenerFactory<K> factory;
            Class<? extends BeeMethodExecutionListenerFactory<K>> listenerFactoryClass = null;
            try {
                listenerFactoryClass = methodExecutionListenerFactoryClass != null ? methodExecutionListenerFactoryClass : loadClass(methodExecutionListenerFactoryClassName);
                factory = createClassInstance(listenerFactoryClass, BeeMethodExecutionListenerFactory.class, "method execution listener factory");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Failed to create method execution listener factory with class[" + methodExecutionListenerClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create method execution listener factory with class[" + listenerFactoryClass + "]", e);
            }

            try {
                return factory.create(this);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create method execution listener by listener factory", e);
            }
        }

        //step4: create a listener
        if (this.methodExecutionListenerClass != null || isNotBlank(this.methodExecutionListenerClassName)) {
            Class<? extends BeeMethodExecutionListener<K>> listenerClass = null;
            try {
                listenerClass = methodExecutionListenerClass != null ? methodExecutionListenerClass : loadClass(methodExecutionListenerClassName);
                return createClassInstance(listenerClass, BeeMethodExecutionListener.class, "object method execution listener");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Failed to create object method execution listener with class[" + methodExecutionListenerClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object method execution listener with class[" + listenerClass + "]", e);
            }
        }
        return null;
    }

    //print check passed configuration
    private void printConfiguration(BeeObjectSourceConfig<K, V> checkedConfig) {
        String poolName = checkedConfig.poolName;
        List<String> exclusionList = checkedConfig.exclusionListOfPrint;
        DefaultLogPrinter.info("................................................BeeOP({})-configuration[start]................................................", poolName);

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
                                DefaultLogPrinter.info("BeeOP({})-config.objectInterfaces=[{}]", poolName, interfacesClassBuf);
                            else
                                DefaultLogPrinter.debug("BeeOP({})-config.objectInterfaces=[{}]", poolName, interfacesClassBuf);
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
                                DefaultLogPrinter.info("BeeOP({})-config.objectInterfaceNames=[{}]", poolName, interfaceNameBuf);
                            else
                                DefaultLogPrinter.debug("BeeOP({})-config.objectInterfaceNames=[{}]", poolName, interfaceNameBuf);
                        }
                        break;
                    }
                    case CONFIG_FACTORY_PROP: {
                        if (!this.objectFactoryProperties.isEmpty()) {
                            if (infoPrint) {
                                for (Map.Entry<String, Object> entry : checkedConfig.objectFactoryProperties.entrySet())
                                    DefaultLogPrinter.info("BeeOP({})-config.objectFactoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            } else {
                                for (Map.Entry<String, Object> entry : checkedConfig.objectFactoryProperties.entrySet())
                                    DefaultLogPrinter.debug("BeeOP({})-config.objectFactoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            }
                        }
                        break;
                    }
                    case CONFIG_EXCLUSION_LIST_OF_PRINT:
                        break;
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

