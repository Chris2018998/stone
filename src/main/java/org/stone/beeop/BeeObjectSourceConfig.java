/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop;

import org.stone.beeop.pool.KeyedObjectPool;
import org.stone.beeop.pool.ObjectPoolStatics;
import org.stone.beeop.pool.ObjectPoolThreadFactory;
import org.stone.tools.CommonUtil;

import java.io.File;
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
    //index for generating default pool name,atomic value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //properties map store entry value injected to object factory
    private final Map<String, Object> factoryProperties = new HashMap<String, Object>(1);

    //if this value is null or empty,a generated pool name set to this field
    private String poolName;
    //fair boolean indicator applied in pool semaphore
    private boolean fairMode;
    //creation size of initial objects
    private int initialSize;
    //async indicator to create initial objects
    private boolean asyncCreateInitObject;
    //max size of sub pools(pool capacity size = (maxObjectKeySize+1) * maxActive)
    private int maxObjectKeySize = 50;
    //max reachable size of pooled objects in sub pools
    private int maxActive = Math.min(Math.max(10, CommonUtil.NCPU), 50);
    //max permit size of pool semaphore
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, CommonUtil.NCPU);
    //milliseconds:max wait time of a borrower to get a idle object from pool,if not get one,then throws an exception
    private long maxWait = SECONDS.toMillis(8);
    //milliseconds:max idle time of pooled objects,if time reached and not be borrowed out,then be removed from pool
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds:max hold time and not be active on borrowed objects,which may be force released to pool
    private long holdTimeout;
    //seconds:max wait time to get a validation result on testing objects
    private int validTestTimeout = 3;
    //milliseconds:max gap time between last activity and borrowed,if less this gap value,assume pooled objects in active state,otherwise test them
    private long validAssumeTime = 500L;
    //milliseconds:interval time to scan idle-timeout objects and hold-timeout objects
    private long timerCheckInterval = MINUTES.toMillis(3);
    //indicator to whether force close using objects when pool clearing
    private boolean forceCloseUsingOnClear;
    //milliseconds:delay time for next loop clearing in pool when exits using objects when<config>forceCloseUsingOnClear</config> is false
    private long delayTimeForNextClear = 3000L;

    //indicator,whether register pool to jmx
    private boolean enableJmx;
    //indicator,whether print pool config info
    private boolean printConfigInfo;
    //indicator,whether print pool runtime info
    private boolean printRuntimeLog;

    //object implements interfaces
    private Class[] objectInterfaces;
    //object implements interface names
    private String[] objectInterfaceNames;

    //class of thread factory(priority-2)
    private Class threadFactoryClass;
    //class name of thread factory(priority-3),if not set,default factory will be applied in pool
    private String threadFactoryClassName = ObjectPoolThreadFactory.class.getName();
    //work thread factory(priority-1)
    private BeeObjectPoolThreadFactory threadFactory;

    //object factory class(RawObjectFactory or RawKeyedObjectFactory)
    private Class objectFactoryClass;
    //object factory class name(RawObjectFactory or RawKeyedObjectFactory)
    private String objectFactoryClassName;
    //object factory(implement class of RawObjectFactory or RawKeyedObjectFactory)
    private RawObjectFactory objectFactory;

    //object method call filter class
    private Class objectMethodFilterClass;
    //object method call filter class name
    private String objectMethodFilterClassName;
    //method call filter instance
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
    //                                     2:configuration about pool inner control(37)                              //
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
        if (maxObjectKeySize >= 2) this.maxObjectKeySize = maxObjectKeySize;
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

    public int getValidTestTimeout() {
        return this.validTestTimeout;
    }

    public void setValidTestTimeout(int validTestTimeout) {
        if (validTestTimeout >= 0) this.validTestTimeout = validTestTimeout;
    }

    public long getValidAssumeTime() {
        return this.validAssumeTime;
    }

    public void setValidAssumeTime(long validAssumeTime) {
        if (validAssumeTime >= 0L) this.validAssumeTime = validAssumeTime;
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

    public void removeFactoryProperty(String key) {
        if (!isBlank(key)) this.factoryProperties.remove(key);
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
        this.loadFromPropertiesFile(new File(filename));
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
        if (maxActive <= 0)
            throw new BeeObjectSourceConfigException("maxActive must be greater than zero");
        if (initialSize < 0)
            throw new BeeObjectSourceConfigException("initialSize must be greater than zero");
        if (initialSize > this.maxActive)
            throw new BeeObjectSourceConfigException("initialSize must not be greater than 'maxActive'");
        if (this.maxObjectKeySize <= 0)
            throw new BeeObjectSourceConfigException("maxObjectKeySize must be greater than zero");
        if (borrowSemaphoreSize <= 0)
            throw new BeeObjectSourceConfigException("borrowSemaphoreSize must be greater than zero");
        if (idleTimeout <= 0L)
            throw new BeeObjectSourceConfigException("idleTimeout must be greater than zero");
        if (holdTimeout < 0L)
            throw new BeeObjectSourceConfigException("holdTimeout must be greater than zero");
        if (maxWait <= 0L)
            throw new BeeObjectSourceConfigException("maxWait must be greater than zero");

        //1:try to create method filter
        RawObjectMethodFilter tempMethodFilter = null;
        if (this.objectMethodFilter == null) tempMethodFilter = this.tryCreateMethodFilter();

        //2:load object implemented interfaces,if config
        Class[] tempObjectInterfaces = this.loadObjectInterfaces();

        //3:try to create object factory
        RawObjectFactory objectFactory = this.tryCreateObjectFactory();
        if (objectFactory.getDefaultKey() == null)
            throw new BeeObjectSourceConfigException("Default key from factory can't be null");
        BeeObjectPoolThreadFactory threadFactory = this.createThreadFactory();

        //4:copy field value to new config from current config
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

        //1:primitive type copy
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
            throw new BeeObjectSourceConfigException("Failed to copy field[" + fieldName + "]", e);
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
                    throw new BeeObjectSourceConfigException("Not found objectInterfaceNames[" + i + "]:" + this.objectInterfaceNames[i]);
                }
            }
            return objectInterfaces;
        }
        return null;
    }

    private RawObjectMethodFilter tryCreateMethodFilter() {
        //1:if exists method filter then return it directly
        if (this.objectMethodFilter != null) return objectMethodFilter;

        //2:if filter class is not null,then try to create instance by it
        if (objectMethodFilterClass != null) {
            try {
                return (RawObjectMethodFilter) ObjectPoolStatics.createClassInstance(objectMethodFilterClass, RawObjectMethodFilter.class, "object method filter");
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object method filter by class:" + objectMethodFilterClass.getName(), e);
            }
        }

        //3:if filter class name is not null,then try to create instance by it
        if (!isBlank(this.objectMethodFilterClassName)) {
            try {
                Class methodFilterClass = Class.forName(this.objectMethodFilterClassName);
                return (RawObjectMethodFilter) ObjectPoolStatics.createClassInstance(methodFilterClass, RawObjectMethodFilter.class, "object method filter");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object filter class:" + this.objectMethodFilterClassName);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object method filter by class:" + objectMethodFilterClassName, e);
            }
        }

        return null;
    }

    private RawObjectFactory tryCreateObjectFactory() {
        //1:if exists object factory,then return it
        if (this.objectFactory != null) return this.objectFactory;

        RawObjectFactory rawObjectFactory = null;
        //2:if factory class exists,then try to create by it
        if (objectFactoryClass != null) {
            try {
                rawObjectFactory = (RawObjectFactory) ObjectPoolStatics.createClassInstance(objectFactoryClass, RawObjectFactory.class, "object factory");
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object factory by class:" + objectFactoryClass.getName(), e);
            }
        }

        //3:if factory class name exists,then try to create by class name
        if (rawObjectFactory == null && !isBlank(this.objectFactoryClassName)) {
            try {
                Class objectFactoryClass = Class.forName(this.objectFactoryClassName);
                rawObjectFactory = (RawObjectFactory) ObjectPoolStatics.createClassInstance(objectFactoryClass, RawObjectFactory.class, "object factory");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object factory class:" + this.objectFactoryClassName);
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object factory by class:" + objectFactoryClassName, e);
            }
        }

        //4:return factory instance
        if (rawObjectFactory != null) {
            //set properties to factory
            if (!factoryProperties.isEmpty())
                ObjectPoolStatics.setPropertiesValue(rawObjectFactory, this.factoryProperties);
            return rawObjectFactory;
        } else {
            throw new BeeObjectSourceConfigException("Must set one of properties['objectFactoryClassName','objectClassName']");
        }
    }

    //create Thread factory
    private BeeObjectPoolThreadFactory createThreadFactory() throws BeeObjectSourceConfigException {
        //step1:if exists thread factory,then return it
        if (this.threadFactory != null) return this.threadFactory;

        //step2: configuration of thread factory
        if (this.threadFactoryClass == null && isBlank(this.threadFactoryClassName))
            throw new BeeObjectSourceConfigException("Configuration item(threadFactoryClass and threadFactoryClassName) can't be null at same time");

        //step3: create thread factory by class or class name
        try {
            Class<?> threadFactClass = this.threadFactoryClass != null ? this.threadFactoryClass : Class.forName(this.threadFactoryClassName);
            return (BeeObjectPoolThreadFactory) createClassInstance(threadFactClass, ObjectPoolThreadFactory.class, "pool thread factory");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BeeObjectSourceConfigException("Failed to create pool thread factory by class:" + this.threadFactoryClassName, e);
        }
    }
}

