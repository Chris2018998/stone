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

import org.stone.beeop.pool.KeyedObjectPool;
import org.stone.tools.CommonUtil;
import org.stone.tools.exception.BeanException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
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
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfig implements BeeObjectSourceConfigMBean {
    //an int sequence for pool names generation,its value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //a map store value of putted items,which are injected to an object factory,default is empty
    private final Map<String, Object> factoryProperties = new HashMap<>();

    //if not set,a generation name assigned to it,default is null
    private String poolName;
    //an indicator of pool work mode,default is false(unfair)
    private boolean fairMode;
    //creation size of initial objects,default is zero
    private int initialSize;
    //creation mode on initial objects;default is false(synchronization mode)
    private boolean asyncCreateInitObject;
    //max key size(pool capacity size = maxObjectKeySize * maxActive),default is 50
    private int maxObjectKeySize = 50;
    //maximum of object instances in pool,its original value is calculated with an expression
    private int maxActive = Math.min(Math.max(10, CommonUtil.NCPU), 50);
    //max permit size of pool semaphore,its original value is calculated with an expression
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, CommonUtil.NCPU);
    //milliseconds: max wait time to get an object instance for a borrower in pool,default is 8000 milliseconds(8 seconds)
    private long maxWait = SECONDS.toMillis(8L);

    //milliseconds: max idle time of un-borrowed object,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = MINUTES.toMillis(3L);
    //milliseconds: max inactive time of borrowed object,which can be recycled by force,default is zero
    private long holdTimeout;

    //seconds: max wait time to get alive test result from objects,default is 3 seconds.
    private int aliveTestTimeout = 3;
    //milliseconds: a threshold time of alive test when borrowed success,if time gap value since last access is less than it,no test on objects,default is 500 milliseconds
    private long aliveAssumeTime = 500L;
    //milliseconds: an interval time that pool scans out timeout objects(idle timeout and hold timeout),default is 18000 milliseconds(3 minutes)
    private long timerCheckInterval = MINUTES.toMillis(3L);
    //an indicator that close borrowed objects immediately,or that close them when them return to pool when clean pool and close pool,default is false.
    private boolean forceCloseUsingOnClear;
    //milliseconds: a park time for waiting borrowed objects return to pool when clean pool and close pool,default is 3000 milliseconds
    private long parkTimeForRetry = 3000L;

    //an indicator to use thread local cache or not(set false to support virtual threads)
    private boolean enableThreadLocal = true;
    //an indicator to register pool to MBean server or not
    private boolean enableJmx;
    //enable indicator to print pool runtime log,default is false
    private boolean printRuntimeLog;
    //enable indicator to print configuration items on pool initialization,default is false
    private boolean printConfigInfo;
    //a skip list on configuration info-print,original items are copied from default skip list
    private List<String> configPrintExclusionList;

    //an array of interfaces implemented by pooled object class
    private Class<?>[] objectInterfaces;
    //an array of interface names of pooled object class
    private String[] objectInterfaceNames;

    //object factory to create pooled object for pool,first priority to used it in pool
    private BeeObjectFactory objectFactory;
    //class of object factory,whose instance can be created when set,second priority to used it in pool
    private Class<? extends BeeObjectFactory> objectFactoryClass;
    //class name of object factory,whose instance can be created when set,third priority to used it in pool
    private String objectFactoryClassName;

    //filter on method invocation,first priority to used it in pool
    private BeeObjectMethodFilter objectMethodFilter;
    //class of filter on method invocation,second priority to used it in pool
    private Class<? extends BeeObjectMethodFilter> objectMethodFilterClass;
    //class name of filter on method invocation,third priority to used it in pool
    private String objectMethodFilterClassName;

    //object predicate
    private BeeObjectPredicate objectPredicate;
    //object predicate class
    private Class<? extends BeeObjectPredicate> objectPredicateClass;
    //object predicate class name
    private String objectPredicateClassName;

    //class name of pool implementation,default is {@code KeyedObjectPool}
    private String poolImplementClassName = KeyedObjectPool.class.getName();


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
        if (initialSize >= 0) this.initialSize = initialSize;
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
        if (maxActive > 0) {
            this.maxActive = maxActive;
            borrowSemaphoreSize = (maxActive > 1) ? Math.min(maxActive / 2, CommonUtil.NCPU) : 1;
        }
    }

    public int getMaxObjectKeySize() {
        return maxObjectKeySize;
    }

    public void setMaxObjectKeySize(int maxObjectKeySize) {
        if (maxObjectKeySize > 0) this.maxObjectKeySize = maxObjectKeySize;
    }

    public int getBorrowSemaphoreSize() {
        return this.borrowSemaphoreSize;
    }

    public void setBorrowSemaphoreSize(int borrowSemaphoreSize) {
        if (borrowSemaphoreSize > 0) this.borrowSemaphoreSize = borrowSemaphoreSize;
    }

    public long getMaxWait() {
        return this.maxWait;
    }

    public void setMaxWait(long maxWait) {
        if (maxWait > 0L) this.maxWait = maxWait;
    }

    public long getIdleTimeout() {
        return this.idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout > 0L) this.idleTimeout = idleTimeout;
    }

    public long getHoldTimeout() {
        return this.holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout >= 0L) this.holdTimeout = holdTimeout;
    }

    public int getAliveTestTimeout() {
        return this.aliveTestTimeout;
    }

    public void setAliveTestTimeout(int aliveTestTimeout) {
        if (aliveTestTimeout >= 0) this.aliveTestTimeout = aliveTestTimeout;
    }

    public long getAliveAssumeTime() {
        return this.aliveAssumeTime;
    }

    public void setAliveAssumeTime(long aliveAssumeTime) {
        if (aliveAssumeTime >= 0L) this.aliveAssumeTime = aliveAssumeTime;
    }

    public long getTimerCheckInterval() {
        return this.timerCheckInterval;
    }

    public void setTimerCheckInterval(long timerCheckInterval) {
        if (timerCheckInterval > 0L) this.timerCheckInterval = timerCheckInterval;
    }

    public boolean isForceCloseUsingOnClear() {
        return this.forceCloseUsingOnClear;
    }

    public void setForceCloseUsingOnClear(boolean forceCloseUsingOnClear) {
        this.forceCloseUsingOnClear = forceCloseUsingOnClear;
    }

    public long getParkTimeForRetry() {
        return this.parkTimeForRetry;
    }

    public void setParkTimeForRetry(long parkTimeForRetry) {
        if (parkTimeForRetry >= 0L) this.parkTimeForRetry = parkTimeForRetry;
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

    public BeeObjectFactory getObjectFactory() {
        return this.objectFactory;
    }

    public void setObjectFactory(BeeObjectFactory factory) {
        this.objectFactory = factory;
    }

    public Class<?> getObjectFactoryClass() {
        return this.objectFactoryClass;
    }

    public void setObjectFactoryClass(Class<? extends BeeObjectFactory> objectFactoryClass) {
        this.objectFactoryClass = objectFactoryClass;
    }

    public String getObjectFactoryClassName() {
        return this.objectFactoryClassName;
    }

    public void setObjectFactoryClassName(String objectFactoryClassName) {
        this.objectFactoryClassName = trimString(objectFactoryClassName);
    }

    public Class<? extends BeeObjectMethodFilter> getObjectMethodFilterClass() {
        return objectMethodFilterClass;
    }

    public void setObjectMethodFilterClass(Class<? extends BeeObjectMethodFilter> filterClass) {
        this.objectMethodFilterClass = filterClass;
    }

    public String getObjectMethodFilterClassName() {
        return objectMethodFilterClassName;
    }

    public void setObjectMethodFilterClassName(String objectMethodFilterClassName) {
        this.objectMethodFilterClassName = objectMethodFilterClassName;
    }

    public BeeObjectMethodFilter getObjectMethodFilter() {
        return objectMethodFilter;
    }

    public void setObjectMethodFilter(BeeObjectMethodFilter objectMethodFilter) {
        this.objectMethodFilter = objectMethodFilter;
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
    //                                     3: pool work configuration(2)                                             //
    //***************************************************************************************************************//
    public String getPoolImplementClassName() {
        return this.poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (isNotBlank(poolImplementClassName))
            this.poolImplementClassName = trimString(poolImplementClassName);
    }

    //***************************************************************************************************************//
    //                                     4: configuration file load(3)                                             //
    //***************************************************************************************************************//
    public void loadFromPropertiesFile(String filename) {
        if (isBlank(filename))
            throw new IllegalArgumentException("Configuration file name can't be null or empty");
        String fileLowerCaseName = filename.toLowerCase(Locale.US);
        if (!fileLowerCaseName.endsWith(".properties"))
            throw new IllegalArgumentException("Configuration file name file must be end with '.properties'");

        if (fileLowerCaseName.startsWith("cp:")) {//1:'cp:' prefix
            String cpFileName = fileLowerCaseName.substring("cp:".length());
            Properties fileProperties = loadPropertiesFromClassPathFile(cpFileName);
            loadFromProperties(fileProperties);
        } else if (fileLowerCaseName.startsWith("classpath:")) {//2:'classpath:' prefix
            String cpFileName = fileLowerCaseName.substring("classpath:".length());
            Properties fileProperties = loadPropertiesFromClassPathFile(cpFileName);
            loadFromProperties(fileProperties);
        } else {//load a real path
            File file = new File(filename);
            if (!file.exists()) throw new IllegalArgumentException("Not found configuration file:" + filename);
            if (!file.isFile())
                throw new IllegalArgumentException("Target object is a valid configuration file," + filename);
            loadFromPropertiesFile(file);
        }
    }

    public void loadFromPropertiesFile(File file) {
        if (file == null) throw new IllegalArgumentException("Configuration properties file can't be null");
        if (!file.exists()) throw new IllegalArgumentException("Configuration properties file not found:" + file);
        if (!file.isFile()) throw new IllegalArgumentException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IllegalArgumentException("Target file is not a properties file");

        try (InputStream stream = Files.newInputStream(file.toPath())) {
            Properties configProperties = new Properties();
            configProperties.load(stream);
            this.loadFromProperties(configProperties);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load configuration file:" + file, e);
        }
    }

    public void loadFromProperties(Properties configProperties) {
        if (configProperties == null || configProperties.isEmpty())
            throw new IllegalArgumentException("Configuration properties can't be null or empty");

        //1: load configuration item values from outside properties
        Map<String, String> setValueMap;
        synchronized (configProperties) {//synchronization mode
            Set<Map.Entry<Object, Object>> entrySet = configProperties.entrySet();
            setValueMap = new HashMap<>(entrySet.size());
            for (Map.Entry<Object, Object> entry : entrySet) {
                setValueMap.put((String) entry.getKey(), (String) entry.getValue());
            }
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
                    objectInterfaces[i] = Class.forName(objectInterfaceNameArray[i]);
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
    //                                     5: configuration check and object factory create methods(4)               //
    //***************************************************************************************************************//
    //check pool configuration
    public BeeObjectSourceConfig check() {
        if (initialSize > this.maxActive)
            throw new BeeObjectSourceConfigException("initial-size must not be greater than max-active");

        //1: try to create object factory
        BeeObjectFactory objectFactory = this.createObjectFactory();
        if (objectFactory.getDefaultKey() == null)
            throw new BeeObjectSourceConfigException("Object factory must provide a non null default pooled key");

        //2: try to load interfaces
        Class<?>[] objectInterfaces = this.loadObjectInterfaces();

        //3: create predicate and filter
        BeeObjectPredicate predicate = this.createObjectPredicate();
        BeeObjectMethodFilter methodFilter = this.tryCreateMethodFilter();

        //4: create a copy from this current configuration object
        BeeObjectSourceConfig checkedConfig = new BeeObjectSourceConfig();
        copyTo(checkedConfig);

        //5: assign above objects to the checked configuration object(such as factory,filter,predicate)
        checkedConfig.objectFactory = objectFactory;
        if (predicate != null) checkedConfig.objectPredicate = predicate;
        if (methodFilter != null) checkedConfig.objectMethodFilter = methodFilter;
        if (objectInterfaces != null) checkedConfig.objectInterfaces = objectInterfaces;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "KeyPool-" + PoolNameIndex.getAndIncrement();
        if (checkedConfig.printConfigInfo) printConfiguration(checkedConfig);
        return checkedConfig;
    }

    void copyTo(BeeObjectSourceConfig config) {
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
                if (!objectInterfaces[i].isInterface())
                    throw new BeeObjectSourceConfigException("Object interfaces[" + i + "]is not a valid interface");
            }
            return objectInterfaces.clone();
        }

        //2: try to load interfaces by names
        if (this.objectInterfaceNames != null && objectInterfaceNames.length > 0) {
            Class<?>[] objectInterfaces = new Class[this.objectInterfaceNames.length];
            for (int i = 0; i < this.objectInterfaceNames.length; i++) {
                try {
                    if (isBlank(this.objectInterfaceNames[i]))
                        throw new BeeObjectSourceConfigException("Object interface class names[" + i + "]is empty or null");
                    objectInterfaces[i] = Class.forName(this.objectInterfaceNames[i]);
                } catch (ClassNotFoundException e) {
                    throw new BeeObjectSourceConfigException("Not found interface class with class names[" + i + "]", e);
                }
            }
            return objectInterfaces;
        }
        return null;
    }

    private BeeObjectMethodFilter tryCreateMethodFilter() {
        //1:if exists method filter then return it directly
        if (this.objectMethodFilter != null) return objectMethodFilter;

        //2: create method filter
        if (objectMethodFilterClass != null || isNotBlank(objectMethodFilterClassName)) {
            Class<?> filterClass = null;
            try {
                filterClass = objectMethodFilterClass != null ? objectMethodFilterClass : Class.forName(objectMethodFilterClassName);
                return (BeeObjectMethodFilter) createClassInstance(filterClass, BeeObjectMethodFilter.class, "object method filter");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object filter class:" + objectMethodFilterClassName);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object method filter by class:" + filterClass, e);
            }
        }

        return null;
    }

    private BeeObjectFactory createObjectFactory() {
        //1: copy from member field of configuration
        BeeObjectFactory rawObjectFactory = this.objectFactory;

        //2: create factory instance
        if (rawObjectFactory == null && (objectFactoryClass != null || objectFactoryClassName != null)) {
            Class<?> factoryClass = null;
            try {
                factoryClass = objectFactoryClass != null ? objectFactoryClass : Class.forName(objectFactoryClassName);
                rawObjectFactory = (BeeObjectFactory) createClassInstance(factoryClass, BeeObjectFactory.class, "object factory");
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
                predicationClass = objectPredicateClass != null ? objectPredicateClass : Class.forName(objectPredicateClassName);
                return (BeeObjectPredicate) createClassInstance(predicationClass, BeeObjectPredicate.class, "object predicate");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found predicate class:" + objectPredicateClassName, e);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create predicate instance with class:" + predicationClass, e);
            }
        }
        return null;
    }

    //print check passed configuration
    private void printConfiguration(BeeObjectSourceConfig checkedConfig) {
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
                                if (interfacesClassBuf.length() > 0) interfacesClassBuf.append(",");
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
                                if (interfaceNameBuf.length() > 0) interfaceNameBuf.append(",");
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

