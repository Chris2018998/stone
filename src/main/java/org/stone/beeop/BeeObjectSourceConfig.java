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
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.tools.BeanUtil.*;
import static org.stone.tools.CommonUtil.*;

/**
 * Bee object source configuration object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfig implements BeeObjectSourceConfigMBean {
    //pool name generation index which is an atomic integer start with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //object factory properties map
    private final Map<String, Object> factoryProperties = new HashMap<>();

    //if null or empty,a generation name will be assigned to it after configuration check passed
    private String poolName;
    //work mode of pool semaphore,default:unfair mode
    private boolean fairMode;
    //creation size of initial objects,default is zero
    private int initialSize;
    //creation mode on initial objects;default is false(synchronization mode)
    private boolean asyncCreateInitObject;
    //max key size(pool capacity size = maxObjectKeySize * maxActive),default is 50
    private int maxObjectKeySize = 50;
    //maximum of objects in instance pool,default is 10(default range: 10 =< number <=50)
    private int maxActive = Math.min(Math.max(10, CommonUtil.NCPU), 50);
    //max permits size of instance pool semaphore
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, CommonUtil.NCPU);
    //milliseconds: max wait time in pool to get objects for borrowers,default is 8000 milliseconds(8 seconds)
    private long maxWait = SECONDS.toMillis(8);

    //milliseconds: max idle time,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds: max inactive time check on borrowed objects,if timeout,pool recycled them by force to avoid objects leak,default is zero
    private long holdTimeout;

    //seconds: max wait time to get validation result on test connections,default is 3 seconds
    private int aliveTestTimeout = 3;
    //milliseconds: a gap time value from last activity time to borrowed time point,needn't do test on objects,default is 500 milliseconds
    private long aliveAssumeTime = 500L;
    //milliseconds: an interval time to scan idle objects or long time hold objects,default is 18000 milliseconds(3 minutes)
    private long timerCheckInterval = MINUTES.toMillis(3);
    //indicator on direct closing borrowed objects while pool clears,default is false
    private boolean forceCloseUsingOnClear;
    //milliseconds: A wait time for borrowed objects return to pool in a loop,at end of wait,try to close returned objects,default is 3000 milliseconds
    private long delayTimeForNextClear = 3000L;


    //thread local cache enable,default is true(set to be false to support virtual threads)
    private boolean enableThreadLocal = true;
    //enable indicator to register configuration and pool to Jmx,default is false
    private boolean enableJmx;
    //enable indicator to print pool runtime log,default is false
    private boolean printRuntimeLog;
    //enable indicator to print configuration items on pool initialization,default is false
    private boolean printConfigInfo;
    //exclusion list on config items print,default is null
    private List<String> configPrintExclusionList;


    //object interfaces
    private Class[] objectInterfaces;
    //object interface names
    private String[] objectInterfaceNames;

    //object factory(priority-1)
    private BeeObjectFactory objectFactory;
    //object factory class(priority-2)
    private Class<? extends BeeObjectFactory> objectFactoryClass;
    //object factory class name(priority-3)
    private String objectFactoryClassName;

    //method call filter(priority-1)
    private BeeObjectMethodFilter objectMethodFilter;
    //object method call filter class(priority-2)
    private Class<? extends BeeObjectMethodFilter> objectMethodFilterClass;
    //object method call filter class name(priority-3)
    private String objectMethodFilterClassName;

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

    public long getDelayTimeForNextClear() {
        return this.delayTimeForNextClear;
    }

    public void setDelayTimeForNextClear(long delayTimeForNextClear) {
        if (delayTimeForNextClear >= 0L) this.delayTimeForNextClear = delayTimeForNextClear;
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
        if (configPrintExclusionList == null) {
            configPrintExclusionList = new ArrayList<>(1);
            configPrintExclusionList.add(fieldName);
        } else if (!configPrintExclusionList.contains(fieldName)) {
            configPrintExclusionList.add(fieldName);
        }
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
    public Class[] getObjectInterfaces() {
        return objectInterfaces;
    }

    public void setObjectInterfaces(Class[] interfaces) {
        this.objectInterfaces = interfaces;
    }

    public String[] getObjectInterfaceNames() {
        return this.objectInterfaceNames;
    }

    public void setObjectInterfaceNames(String[] interfaceNames) {
        this.objectInterfaceNames = interfaceNames;
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

    public BeeObjectFactory getObjectFactory() {
        return this.objectFactory;
    }

    public void setRawObjectFactory(BeeObjectFactory factory) {
        this.objectFactory = factory;
    }

    public Class getObjectMethodFilterClass() {
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
        String fileLowerCaseName = filename.toLowerCase(Locale.US);
        if (!fileLowerCaseName.endsWith(".properties"))
            throw new IllegalArgumentException("Configuration file name file must end with '.properties'");

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

        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            Properties configProperties = new Properties();
            configProperties.load(stream);
            this.loadFromProperties(configProperties);
        } catch (BeeObjectSourceConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to load configuration properties file:" + file, e);
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
            Map<String, String> setValueMap = new HashMap<>(configProperties.size());
            for (String propertyName : configProperties.stringPropertyNames()) {
                setValueMap.put(propertyName, configProperties.getProperty(propertyName));
            }

            //2:inject item value from map to this dataSource config object
            try {
                setPropertiesValue(this, setValueMap);
            } catch (BeanException e) {
                throw new BeeObjectSourceConfigException(e.getMessage(), e);
            }

            //3:try to find 'factoryProperties' config value
            this.addFactoryProperty(getPropertyValue(setValueMap, "factoryProperties"));
            String factoryPropertiesSize = getPropertyValue(setValueMap, "factoryProperties.size");
            if (isNotBlank(factoryPropertiesSize)) {
                int size = 0;
                try {
                    size = Integer.parseInt(factoryPropertiesSize.trim());
                } catch (Throwable e) {
                    //do nothing
                }
                for (int i = 1; i <= size; i++)
                    this.addFactoryProperty(getPropertyValue(setValueMap, "factoryProperties." + i));
            }

            //5:try to find 'objectInterfaceNames' config value
            String objectInterfaceNames = getPropertyValue(setValueMap, "objectInterfaceNames");
            if (isNotBlank(objectInterfaceNames))
                setObjectInterfaceNames(objectInterfaceNames.split(","));

            //6:try to find 'objectInterfaces' config value
            String objectInterfaceNames2 = getPropertyValue(setValueMap, "objectInterfaces");
            if (isNotBlank(objectInterfaceNames2)) {
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
        BeeObjectFactory objectFactory = this.createObjectFactory();
        if (objectFactory.getDefaultKey() == null)
            throw new BeeObjectSourceConfigException("Default key from factory can't be null");

        //2:try to create method filter
        BeeObjectMethodFilter tempMethodFilter = this.tryCreateMethodFilter();

        //3:load object implemented interfaces
        Class[] tempObjectInterfaces = this.loadObjectInterfaces();

        //4:create a checked configuration and copy local fields to it
        BeeObjectSourceConfig checkedConfig = new BeeObjectSourceConfig();
        copyTo(checkedConfig);

        //5:set temp to config
        checkedConfig.objectFactory = objectFactory;
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
                    CommonLog.info("{}.{}={}", this.poolName, fieldName, fieldValue);
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
                    CommonLog.info("{}.objectInterfaces[{}]={}", this.poolName, i, interfaces[i]);
            config.setObjectInterfaces(interfaces);
        }

        //3:copy 'objectInterfaceNames'
        String[] interfaceNames = (this.objectInterfaceNames == null) ? null : new String[this.objectInterfaceNames.length];
        if (interfaceNames != null) {
            System.arraycopy(this.objectInterfaceNames, 0, interfaceNames, 0, interfaceNames.length);
            for (int i = 0, l = this.objectInterfaceNames.length; i < l; i++)
                if (this.printConfigInfo)
                    CommonLog.info("{}.objectInterfaceNames[{}]={}", this.poolName, i, this.objectInterfaceNames[i]);
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

    private BeeObjectMethodFilter tryCreateMethodFilter() {
        //1:if exists method filter then return it directly
        if (this.objectMethodFilter != null) return objectMethodFilter;

        //2: create method filter
        if (objectMethodFilterClass != null || isNotBlank(objectMethodFilterClassName)) {
            Class filterClass = null;
            try {
                filterClass = objectMethodFilterClass != null ? objectMethodFilterClass : Class.forName(objectMethodFilterClassName);
                return (BeeObjectMethodFilter) createClassInstance(filterClass, BeeObjectMethodFilter.class, "object method filter");
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

    private BeeObjectFactory createObjectFactory() {
        //1: copy from member field of configuration
        BeeObjectFactory rawObjectFactory = this.objectFactory;

        //2: create factory instance
        if (rawObjectFactory == null && (objectFactoryClass != null || objectFactoryClassName != null)) {
            Class factoryClass = null;
            try {
                factoryClass = objectFactoryClass != null ? objectFactoryClass : Class.forName(objectFactoryClassName);
                rawObjectFactory = (BeeObjectFactory) createClassInstance(factoryClass, BeeObjectFactory.class, "object factory");
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
            try {
                setPropertiesValue(rawObjectFactory, factoryProperties);
            } catch (BeanException e) {
                throw new BeeObjectSourceConfigException(e.getMessage(), e);
            }

        return rawObjectFactory;
    }
}

