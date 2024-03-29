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
import org.stone.beeop.pool.ObjectPoolStatics;
import org.stone.beeop.pool.PoolThreadFactory;
import org.stone.tools.CommonUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beeop.pool.ObjectPoolStatics.createClassInstance;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.trimString;

/**
 * Configuration of bee object source
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfig implements BeeObjectSourceConfigJmxBean {
    //atomic index at pool name generation,its value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //store properties which are injected to object factory while pool initialization
    private final Map<String, Object> factoryProperties = new HashMap<String, Object>();

    //a name assign to pool,if null or empty,then set a generated name to pool on initialization
    private String poolName;
    //enable pool semaphore works in fair mode
    private boolean fairMode;
    //creation size of objects on pool starting up
    private int initialSize;
    //indicator on creating initial objects with a async thread
    private boolean asyncCreateInitObject;
    //max object key size(pool capacity size = (maxObjectKeySize * maxActive)
    private int maxObjectKeySize = 50;
    //max reachable size of object instance by per key
    private int maxActive = Math.min(Math.max(10, CommonUtil.NCPU), 50);
    //max permit size of semaphore by per key
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, CommonUtil.NCPU);
    //milliseconds:max wait time of a borrower to get a idle object from pool,if not get one,then throws an exception
    private long maxWait = SECONDS.toMillis(8);
    //milliseconds:max idle time of pooled objects,if time reached and not be borrowed out,then be removed from pool
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds:max hold time and not be active on borrowed objects,which may be force released to pool
    private long holdTimeout;
    //seconds:max wait time to get a validation result on testing objects
    private int aliveTestTimeout = 3;
    //milliseconds:max gap time between last activity and borrowed,if less this gap value,assume pooled objects in active state,otherwise test them
    private long aliveAssumeTime = 500L;
    //milliseconds:interval time to scan idle-timeout objects and hold-timeout objects
    private long timerCheckInterval = MINUTES.toMillis(3);
    //indicator to whether force close using objects when pool clearing
    private boolean forceCloseUsingOnClear;
    //milliseconds:delay time for next loop clearing in pool when exits using objects when<config>forceCloseUsingOnClear</config> is false
    private long delayTimeForNextClear = 3000L;

    //jmx register indicator
    private boolean enableJmx;
    //config info print indicator while pool initialization
    private boolean printConfigInfo;
    //pool runtime info print indicator,which can be changed by calling setPrintRuntimeLog method at runtime
    private boolean printRuntimeLog;

    //interfaces implemented by pooled object
    private Class[] objectInterfaces;
    //names of interfaces implemented by pooled object
    private String[] objectInterfaceNames;

    //class of thread factory(priority-2)
    private Class threadFactoryClass;
    //class name of thread factory(priority-3),if not set,default factory will be applied in pool
    private String threadFactoryClassName = PoolThreadFactory.class.getName();
    //work thread factory(priority-1)
    private BeeObjectPoolThreadFactory threadFactory;

    //object factory class(priority-2)
    private Class objectFactoryClass;
    //object factory class name(priority-3)
    private String objectFactoryClassName;
    //object factory(priority-1)
    private RawObjectFactory objectFactory;

    //object method call filter class(priority-2)
    private Class objectMethodFilterClass;
    //object method call filter class name(priority-3)
    private String objectMethodFilterClassName;
    //method call filter(priority-1)
    private RawObjectMethodFilter objectMethodFilter;

    //pool implementation class name
    private String poolImplementClassName = KeyedObjectPool.class.getName();

    //***************************************************************************************************************//
    //                                     1: constructors(4)                                                        //
    //***************************************************************************************************************//
    public BeeObjectSourceConfig() {
    }

    //load configuration from properties file
    public BeeObjectSourceConfig(File propertiesFile) {
        loadFromPropertiesFile(propertiesFile);
    }

    //load configuration from properties file
    public BeeObjectSourceConfig(String propertiesFileName) {
        loadFromPropertiesFile(propertiesFileName);
    }

    //load configuration from properties
    public BeeObjectSourceConfig(Properties configProperties) {
        loadFromProperties(configProperties);
    }

    //***************************************************************************************************************//
    //                                     2: base configuration(37)                                                 //
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
        if (maxObjectKeySize >= 1) this.maxObjectKeySize = maxObjectKeySize;
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
        this.holdTimeout = holdTimeout;
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

    public long getDelayTimeForNextClear() {
        return this.delayTimeForNextClear;
    }

    public void setDelayTimeForNextClear(long delayTimeForNextClear) {
        if (delayTimeForNextClear >= 0L) this.delayTimeForNextClear = delayTimeForNextClear;
    }

    public String getPoolImplementClassName() {
        return this.poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (!isBlank(poolImplementClassName))
            this.poolImplementClassName = trimString(poolImplementClassName);
    }

    public boolean isEnableJmx() {
        return this.enableJmx;
    }

    public void setEnableJmx(boolean enableJmx) {
        this.enableJmx = enableJmx;
    }

    public void setPrintConfigInfo(boolean printConfigInfo) {
        this.printConfigInfo = printConfigInfo;
    }

    public boolean isPrintRuntimeLog() {
        return this.printRuntimeLog;
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        this.printRuntimeLog = printRuntimeLog;
    }

    //***************************************************************************************************************//
    //                                     3: configuration about object creation(19)                                //
    //***************************************************************************************************************//
    public Class[] getObjectInterfaces() {
        return (this.objectInterfaces == null) ? ObjectPoolStatics.EMPTY_CLASSES : objectInterfaces.clone();
    }

    public void setObjectInterfaces(Class[] interfaces) {
        if (interfaces == null || interfaces.length == 0) {
            this.objectInterfaces = null;
        } else {
            this.objectInterfaces = interfaces.clone();
        }
    }

    public String[] getObjectInterfaceNames() {
        return this.objectInterfaceNames == null ? ObjectPoolStatics.EMPTY_CLASS_NAMES : objectInterfaceNames.clone();
    }

    public void setObjectInterfaceNames(String[] interfaceNames) {
        if (interfaceNames == null || interfaceNames.length == 0) {
            objectInterfaceNames = null;
        } else {
            objectInterfaceNames = interfaceNames.clone();
        }
    }

    public Class getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(Class threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
    }

    public String getThreadFactoryClassName() {
        return threadFactoryClassName;
    }

    public void setThreadFactoryClassName(String threadFactoryClassName) {
        this.threadFactoryClassName = threadFactoryClassName;
    }

    public BeeObjectPoolThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(BeeObjectPoolThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public Class getObjectFactoryClass() {
        return this.objectFactoryClass;
    }

    public void setObjectFactoryClass(Class objectFactoryClass) {
        this.objectFactoryClass = objectFactoryClass;
    }

    public String getObjectFactoryClassName() {
        return this.objectFactoryClassName;
    }

    public void setObjectFactoryClassName(String objectFactoryClassName) {
        this.objectFactoryClassName = trimString(objectFactoryClassName);
    }

    public RawObjectFactory getObjectFactory() {
        return this.objectFactory;
    }

    public void setRawObjectFactory(RawObjectFactory factory) {
        this.objectFactory = factory;
    }

    public Class getObjectMethodFilterClass() {
        return objectMethodFilterClass;
    }

    public void setObjectMethodFilterClass(Class objectMethodFilterClass) {
        this.objectMethodFilterClass = objectMethodFilterClass;
    }

    public String getObjectMethodFilterClassName() {
        return objectMethodFilterClassName;
    }

    public void setObjectMethodFilterClassName(String objectMethodFilterClassName) {
        this.objectMethodFilterClassName = objectMethodFilterClassName;
    }

    public RawObjectMethodFilter getObjectMethodFilter() {
        return objectMethodFilter;
    }

    public void setObjectMethodFilter(RawObjectMethodFilter objectMethodFilter) {
        this.objectMethodFilter = objectMethodFilter;
    }

    public Object getFactoryProperty(String key) {
        return this.factoryProperties.get(key);
    }

    public Object removeFactoryProperty(String key) {
        return this.factoryProperties.remove(key);
    }

    public void addFactoryProperty(String key, Object value) {
        if (!isBlank(key) && value != null) this.factoryProperties.put(key, value);
    }

    public void addFactoryProperty(String propertyText) {
        if (!isBlank(propertyText)) {
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
    //                                     4: configuration load from properties load (3)                            //
    //***************************************************************************************************************//
    public void loadFromPropertiesFile(String filename) {
        if (isBlank(filename)) throw new IllegalArgumentException("Configuration properties file can't be null");

        File file = new File(filename);
        if (file.exists()) {
            this.loadFromPropertiesFile(file);
        } else {//try to load config from classpath
            Class selfClass = BeeObjectSourceConfig.class;
            InputStream propertiesStream = selfClass.getResourceAsStream(filename);
            if (propertiesStream == null) propertiesStream = selfClass.getClassLoader().getResourceAsStream(filename);

            Properties prop = new Properties();
            try {
                prop.load(propertiesStream);
            } catch (IOException e) {
                throw new IllegalArgumentException("Configuration properties file load failed", e);
            } finally {
                if (propertiesStream != null) {
                    try {
                        propertiesStream.close();
                    } catch (Throwable e) {
                        //do nothing
                    }
                }
            }

            loadFromProperties(prop);
        }
    }

    public void loadFromPropertiesFile(File file) {
        if (file == null) throw new IllegalArgumentException("Configuration properties file can't be null");
        if (!file.exists()) throw new IllegalArgumentException(file.getAbsolutePath());
        if (!file.isFile()) throw new IllegalArgumentException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IllegalArgumentException("Target file is not a properties file");

        InputStream stream = null;
        try {
            stream = Files.newInputStream(Paths.get(file.toURI()));
            Properties configProperties = new Properties();
            configProperties.load(stream);
            this.loadFromProperties(configProperties);
        } catch (BeeObjectSourceConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to load configuration properties file:", e);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    public void loadFromProperties(Properties configProperties) {
        if (configProperties == null || configProperties.isEmpty())
            throw new IllegalArgumentException("Configuration properties can't be null or empty");

        //1:load configuration item values from outside properties
        synchronized (configProperties) {//synchronization mode
            Map<String, Object> setValueMap = new HashMap<String, Object>(configProperties.size());
            for (String propertyName : configProperties.stringPropertyNames()) {
                setValueMap.put(propertyName, configProperties.getProperty(propertyName));
            }

            //2:inject item value from map to this dataSource config object
            ObjectPoolStatics.setPropertiesValue(this, setValueMap);

            //3:try to find 'factoryProperties' config value
            this.addFactoryProperty(ObjectPoolStatics.getPropertyValue(configProperties, "factoryProperties"));
            String factoryPropertiesSize = ObjectPoolStatics.getPropertyValue(configProperties, "factoryProperties.size");
            if (!isBlank(factoryPropertiesSize)) {
                int size = 0;
                try {
                    size = Integer.parseInt(factoryPropertiesSize.trim());
                } catch (Throwable e) {
                    //do nothing
                }
                for (int i = 1; i <= size; i++)
                    this.addFactoryProperty(ObjectPoolStatics.getPropertyValue(configProperties, "factoryProperties." + i));
            }

            //5:try to find 'objectInterfaceNames' config value
            String objectInterfaceNames = ObjectPoolStatics.getPropertyValue(configProperties, "objectInterfaceNames");
            if (!isBlank(objectInterfaceNames))
                setObjectInterfaceNames(objectInterfaceNames.split(","));

            //6:try to find 'objectInterfaces' config value
            String objectInterfaceNames2 = ObjectPoolStatics.getPropertyValue(configProperties, "objectInterfaces");
            if (!isBlank(objectInterfaceNames2)) {
                String[] objectInterfaceNameArray = objectInterfaceNames2.split(",");
                Class[] objectInterfaces = new Class[objectInterfaceNameArray.length];
                for (int i = 0, l = objectInterfaceNameArray.length; i < l; i++) {
                    try {
                        objectInterfaces[i] = Class.forName(objectInterfaceNameArray[i]);
                    } catch (ClassNotFoundException e) {
                        throw new BeeObjectSourceConfigException("Class not found:" + objectInterfaceNameArray[i]);
                    }
                }
                setObjectInterfaces(objectInterfaces);
            }
        }
    }

    //***************************************************************************************************************//
    //                                     5: configuration check and object factory create methods(4)               //
    //***************************************************************************************************************//
    //check pool configuration
    public BeeObjectSourceConfig check() {
        if (initialSize > this.maxActive)
            throw new BeeObjectSourceConfigException("initialSize must not be greater than 'maxActive'");

        //1:try to create object factory
        RawObjectFactory objectFactory = this.createObjectFactory();
        if (objectFactory.getDefaultKey() == null)
            throw new BeeObjectSourceConfigException("Default key from factory can't be null");

        //2:try to create method filter
        RawObjectMethodFilter tempMethodFilter = this.tryCreateMethodFilter();

        //3:load object implemented interfaces
        Class[] tempObjectInterfaces = this.loadObjectInterfaces();

        //create pool thread factory
        BeeObjectPoolThreadFactory threadFactory = this.createThreadFactory();

        //4:create a checked configuration and copy local fields to it
        BeeObjectSourceConfig checkedConfig = new BeeObjectSourceConfig();
        copyTo(checkedConfig);

        //5:set temp to config
        checkedConfig.objectFactory = objectFactory;
        checkedConfig.threadFactory = threadFactory;
        if (tempMethodFilter != null) checkedConfig.objectMethodFilter = tempMethodFilter;
        if (tempObjectInterfaces != null) checkedConfig.objectInterfaces = tempObjectInterfaces;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "KeyPool-" + PoolNameIndex.getAndIncrement();
        return checkedConfig;
    }

    void copyTo(BeeObjectSourceConfig config) {
        List<String> excludeFieldList = new ArrayList<String>(3);
        excludeFieldList.add("factoryProperties");
        excludeFieldList.add("objectInterfaces");
        excludeFieldList.add("objectInterfaceNames");

        //1:copy primitive type fields
        String fieldName = "";
        try {
            for (Field field : BeeObjectSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()) || excludeFieldList.contains(field.getName()))
                    continue;
                Object fieldValue = field.get(this);
                fieldName = field.getName();

                if (this.printConfigInfo)
                    ObjectPoolStatics.CommonLog.info("{}.{}={}", this.poolName, fieldName, fieldValue);
                field.set(config, fieldValue);
            }
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to filled value on field[" + fieldName + "]", e);
        }

        //2:copy 'objectInterfaces'
        Class[] interfaces = this.objectInterfaces == null ? null : new Class[this.objectInterfaces.length];
        if (interfaces != null) {
            System.arraycopy(this.objectInterfaces, 0, interfaces, 0, interfaces.length);
            for (int i = 0, l = interfaces.length; i < l; i++)
                if (this.printConfigInfo)
                    ObjectPoolStatics.CommonLog.info("{}.objectInterfaces[{}]={}", this.poolName, i, interfaces[i]);
            config.setObjectInterfaces(interfaces);
        }

        //3:copy 'objectInterfaceNames'
        String[] interfaceNames = (this.objectInterfaceNames == null) ? null : new String[this.objectInterfaceNames.length];
        if (interfaceNames != null) {
            System.arraycopy(this.objectInterfaceNames, 0, interfaceNames, 0, interfaceNames.length);
            for (int i = 0, l = this.objectInterfaceNames.length; i < l; i++)
                if (this.printConfigInfo)
                    ObjectPoolStatics.CommonLog.info("{}.objectInterfaceNames[{}]={}", this.poolName, i, this.objectInterfaceNames[i]);
            config.setObjectInterfaceNames(interfaceNames);
        }
    }

    private Class[] loadObjectInterfaces() throws BeeObjectSourceConfigException {
        //1: if objectInterfaces field value is not null,then check it and return it
        if (objectInterfaces != null) {
            for (int i = 0, l = objectInterfaces.length; i < l; i++) {
                if (objectInterfaces[i] == null)
                    throw new BeeObjectSourceConfigException("interfaces array[" + i + "]is null");
                if (!objectInterfaces[i].isInterface())
                    throw new BeeObjectSourceConfigException("interfaces array[" + i + "]is not valid interface");
            }
            return objectInterfaces;
        }

        //2: try to load interfaces by names
        if (this.objectInterfaceNames != null) {
            Class[] objectInterfaces = new Class[this.objectInterfaceNames.length];
            for (int i = 0; i < this.objectInterfaceNames.length; i++) {
                try {
                    if (isBlank(this.objectInterfaceNames[i]))
                        throw new BeeObjectSourceConfigException("objectInterfaceNames[" + i + "]is empty or null");
                    objectInterfaces[i] = Class.forName(this.objectInterfaceNames[i]);
                } catch (ClassNotFoundException e) {
                    throw new BeeObjectSourceConfigException("Not found objectInterfaceNames[" + i + "]:" + this.objectInterfaceNames[i], e);
                }
            }
            return objectInterfaces;
        }
        return null;
    }

    private RawObjectMethodFilter tryCreateMethodFilter() {
        //1:if exists method filter then return it directly
        if (this.objectMethodFilter != null) return objectMethodFilter;

        //2: create method filter
        if (objectMethodFilterClass != null || !isBlank(objectMethodFilterClassName)) {
            Class filterClass = null;
            try {
                filterClass = objectMethodFilterClass != null ? objectMethodFilterClass : Class.forName(objectMethodFilterClassName);
                return (RawObjectMethodFilter) ObjectPoolStatics.createClassInstance(filterClass, RawObjectMethodFilter.class, "object method filter");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object filter class:" + objectMethodFilterClassName);
            } catch (BeeObjectSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object method filter by class:" + filterClass, e);
            }
        }

        return null;
    }

    private RawObjectFactory createObjectFactory() {
        //1: copy from member field of configuration
        RawObjectFactory rawObjectFactory = this.objectFactory;

        //2: create factory instance
        if (rawObjectFactory == null && (objectFactoryClass != null || objectFactoryClassName != null)) {
            Class factoryClass = null;
            try {
                factoryClass = objectFactoryClass != null ? objectFactoryClass : Class.forName(objectFactoryClassName);
                rawObjectFactory = (RawObjectFactory) ObjectPoolStatics.createClassInstance(factoryClass, RawObjectFactory.class, "object factory");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object factory class:" + objectFactoryClassName, e);
            } catch (BeeObjectSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object factory by class:" + factoryClass, e);
            }
        }

        //3: throw check failure exception
        if (rawObjectFactory == null)
            throw new BeeObjectSourceConfigException("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]");

        //4: inject properties to factory
        if (!factoryProperties.isEmpty())
            ObjectPoolStatics.setPropertiesValue(rawObjectFactory, factoryProperties);

        return rawObjectFactory;
    }

    //create Thread factory
    private BeeObjectPoolThreadFactory createThreadFactory() throws BeeObjectSourceConfigException {
        //step1: if exists thread factory,then return it
        if (this.threadFactory != null) return this.threadFactory;

        //step2: configuration of thread factory
        if (this.threadFactoryClass == null && isBlank(this.threadFactoryClassName))
            throw new BeeObjectSourceConfigException("Must provide one of config items[threadFactory,threadFactoryClass,threadFactoryClassName]");

        //step3: create thread factory by class or class name
        Class<?> threadFactClass = null;
        try {
            threadFactClass = this.threadFactoryClass != null ? this.threadFactoryClass : Class.forName(this.threadFactoryClassName);
            return (BeeObjectPoolThreadFactory) createClassInstance(threadFactClass, PoolThreadFactory.class, "pool thread factory");
        } catch (ClassNotFoundException e) {
            throw new BeeObjectSourceConfigException("Not found thread factory class:" + threadFactoryClassName, e);
        } catch (BeeObjectSourceConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to create pool thread factory by class:" + threadFactClass, e);
        }
    }
}

