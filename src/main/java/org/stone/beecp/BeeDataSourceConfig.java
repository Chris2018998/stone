/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import org.stone.beecp.pool.ConnectionFactoryByDriver;
import org.stone.beecp.pool.ConnectionFactoryByDriverDs;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.XaConnectionFactoryByDriverDs;
import org.stone.tools.exception.BeanException;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beecp.BeeTransactionIsolationLevels.TRANS_LEVEL_CODE_LIST;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.BeanUtil.*;
import static org.stone.tools.CommonUtil.*;

/**
 * Bee data source configuration object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeDataSourceConfig implements BeeDataSourceConfigMBean {
    //an int sequence for pool names generation,its value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //a default name list of items to be skipped over log print when pool initializes
    private static final List<String> DefaultExclusionList = Arrays.asList("username", "password", "jdbcUrl", "user", "url");

    //a map store value of putted items,which are injected to a connection factory or a datasource when pool initializes,default is empty
    private final Map<String, Object> connectProperties = new HashMap<>(0);
    //a skip list on configuration info-print,original items are copied from default skip list,refer to {@code DefaultExclusionList}
    private final List<String> configPrintExclusionList = new ArrayList<>(DefaultExclusionList);
    //jdbc username link to a database,default is null
    private String username;
    //jdbc password link to a database,default is null
    private String password;
    //jdbc url link to a database,default is null
    private String jdbcUrl;
    //jdbc driver class name,default is null;if not set,pool try to search a matched driver with non {@code dbcUrl}
    private String driverClassName;
    //if not set,a generation name assigned to it,default is null
    private String poolName;
    //an indicator of pool work mode,default is false(unfair)
    private boolean fairMode;
    //creation size of initial connections,default is zero
    private int initialSize;
    //an indicator of creation way of initial connections,default is false(synchronization)
    private boolean asyncCreateInitConnection;
    //maximum of connections in pool,its original value is calculated with an expression
    private int maxActive = Math.min(Math.max(10, NCPU), 50);
    //max permit size of pool semaphore,its original value is calculated with an expression
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, NCPU);
    //milliseconds: max wait time to get a connection for a borrower in pool,default is 8000 milliseconds(8 seconds)
    private long maxWait = SECONDS.toMillis(8L);
    //milliseconds: max idle time of un-borrowed connections,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = MINUTES.toMillis(3L);
    //milliseconds: max inactive time of borrowed connections,which can be recycled by force,default is zero
    private long holdTimeout;
    //a test sql to validate connection whether alive
    private String aliveTestSql = "SELECT 1";
    //seconds: max wait time to get alive test result from connections,default is 3 seconds.
    private int aliveTestTimeout = 3;
    //milliseconds: a threshold time of alive test when borrowed success,if time gap value since last access is less than it,no test on connections,default is 500 milliseconds
    private long aliveAssumeTime = 500L;
    //milliseconds: an interval time that pool scans out timeout connections(idle timeout and hold timeout),default is 18000 milliseconds(3 minutes)
    private long timerCheckInterval = MINUTES.toMillis(3L);
    //an indicator that close borrowed connections immediately,or that close them when them return to pool when clean pool and close pool,default is false.
    private boolean forceCloseUsingOnClear;
    //milliseconds: a park time for waiting borrowed connections return to pool when clean pool and close pool,default is 3000 milliseconds
    private long parkTimeForRetry = 3000L;
    //a code list for eviction check on sql exceptions
    private List<Integer> sqlExceptionCodeList;
    //a state list for eviction check on sql exceptions
    private List<String> sqlExceptionStateList;
    //an initial value of catalog property on new connections,refer to {@code Connection.setCatalog(String)}
    private String defaultCatalog;
    //an initial value of schema property on new connections,refer to {@code Connection.setSchema(String)}
    private String defaultSchema;
    //an initial value of readOnly property on new connections,refer to {@code Connection.setReadOnly(boolean)}
    private Boolean defaultReadOnly;
    //an initial value of autoCommit property on new connections,refer to {@code Connection.setAutoCommit(boolean)}
    private Boolean defaultAutoCommit;
    //an initial value of transactionIsolation property on new connections,refer to {@code Connection.setTransactionIsolation(int)}
    private Integer defaultTransactionIsolationCode;
    //a name of a transaction isolation code,which is set to {@code defaultTransactionIsolationCode} on pool initialization
    private String defaultTransactionIsolationName;
    //an indicator to use thread local cache or not(set false to support virtual threads)
    private boolean enableThreadLocal = true;
    //an indicator to set initial value to catalog property after connections are created
    private boolean enableDefaultOnCatalog = true;
    //an indicator to set initial value to schema property after connections are created
    private boolean enableDefaultOnSchema = true;
    //an indicator to set initial value to readOnly property after connections are created
    private boolean enableDefaultOnReadOnly = true;
    //an indicator to set initial value to autoCommit property after connections are created
    private boolean enableDefaultOnAutoCommit = true;
    //an indicator to set initial value to transactionIsolation property after connections are created
    private boolean enableDefaultOnTransactionIsolation = true;
    //an indicator that set a dirty flag of schema property to connection and ignore change when call setSchema(String) method on connection
    //this can be used to support some special drivers to recovery schema after transaction end (for example:PG driver)
    private boolean forceDirtyOnSchemaAfterSet;
    //an indicator that set a dirty flag of catalog property to connection and ignore change when call setCatalog(String) method on connection
    //this can be used to support some special drivers to recovery catalog after transaction end (for example:PG driver)
    private boolean forceDirtyOnCatalogAfterSet;
    /**
     * connection factory class,which must be implement one of the below four interfaces
     * 1: <class>RawConnectionFactory</class>
     * 2: <class>RawXaConnectionFactory</class>
     * 3: <class>DataSource</class>
     * 4: <class>XADataSource</class>
     */
    //connection factory instance
    private Object connectionFactory;
    //connection factory class
    private Class<?> connectionFactoryClass;
    //connection factory class name
    private String connectionFactoryClassName;
    /**
     * connections eviction check on thrown sql exceptions by customization
     * eviction check priority logic
     * 1: if exists a predication,only check with predication
     * 2: if not exists,priority order: error code check,sql state check
     */
    //eviction predicate
    private BeeConnectionPredicate evictPredicate;
    //eviction predicate class
    private Class<? extends BeeConnectionPredicate> evictPredicateClass;
    //eviction predicate class name
    private String evictPredicateClassName;
    /**
     * A short lifecycle object and used to decode jdbc link info(url,username,password)in pool initialization check
     */
    //decoder
    private BeeJdbcLinkInfoDecoder jdbcLinkInfoDecoder;
    //decoder class
    private Class<? extends BeeJdbcLinkInfoDecoder> jdbcLinkInfoDecoderClass;
    //decoder class name
    private String jdbcLinkInfoDecoderClassName;
    //enable indicator to register configuration and pool to Jmx,default is false
    private boolean enableJmx;
    //enable indicator to print pool runtime log,default is false
    private boolean printRuntimeLog;
    //enable indicator to print configuration items on pool initialization,default is false
    private boolean printConfigInfo;
    //pool implementation class name,if not be set,a default implementation applied in bee datasource
    private String poolImplementClassName = FastConnectionPool.class.getName();

    //****************************************************************************************************************//
    //                                     1: constructors(5)                                                         //
    //****************************************************************************************************************//
    public BeeDataSourceConfig() {
    }

    //read configuration from properties file
    public BeeDataSourceConfig(File propertiesFile) {
        loadFromPropertiesFile(propertiesFile);
    }

    //read configuration from properties file
    public BeeDataSourceConfig(String propertiesFileName) {
        loadFromPropertiesFile(propertiesFileName);
    }

    //read configuration from properties
    public BeeDataSourceConfig(Properties configProperties) {
        loadFromProperties(configProperties);
    }

    public BeeDataSourceConfig(String driver, String url, String user, String password) {
        this.jdbcUrl = trimString(url);
        this.username = trimString(user);
        this.password = trimString(password);
        this.driverClassName = trimString(driver);
    }

    //****************************************************************************************************************//
    //                                     2: JDBC link configuration methods(10)                                     //
    //****************************************************************************************************************//
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = trimString(username);
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = trimString(password);
    }

    public String getUrl() {
        return this.jdbcUrl;
    }

    public void setUrl(String jdbcUrl) {
        this.jdbcUrl = trimString(jdbcUrl);
    }

    public String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = trimString(jdbcUrl);
    }

    public String getDriverClassName() {
        return this.driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = trimString(driverClassName);
    }


    //****************************************************************************************************************//
    //                                3: configuration about pool inner control(30)                                   //
    //****************************************************************************************************************//
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

    public boolean isAsyncCreateInitConnection() {
        return asyncCreateInitConnection;
    }

    public void setAsyncCreateInitConnection(boolean asyncCreateInitConnection) {
        this.asyncCreateInitConnection = asyncCreateInitConnection;
    }

    public int getMaxActive() {
        return this.maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (maxActive > 0) {
            this.maxActive = maxActive;
            //fix issue:#19 Chris-2020-08-16 begin
            this.borrowSemaphoreSize = maxActive > 1 ? Math.min(maxActive / 2, NCPU) : 1;
            //fix issue:#19 Chris-2020-08-16 end
        }
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

    public String getAliveTestSql() {
        return this.aliveTestSql;
    }

    public void setAliveTestSql(String aliveTestSql) {
        if (isNotBlank(aliveTestSql)) this.aliveTestSql = trimString(aliveTestSql);
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

    public List<Integer> getSqlExceptionCodeList() {
        return sqlExceptionCodeList;
    }

    public void addSqlExceptionCode(int code) {
        if (sqlExceptionCodeList == null) sqlExceptionCodeList = new ArrayList<>(1);
        if (!this.sqlExceptionCodeList.contains(code)) this.sqlExceptionCodeList.add(code);
    }

    public void removeSqlExceptionCode(int code) {
        if (sqlExceptionCodeList != null) this.sqlExceptionCodeList.remove(Integer.valueOf(code));
    }

    public List<String> getSqlExceptionStateList() {
        return sqlExceptionStateList;
    }

    public void addSqlExceptionState(String state) {
        if (sqlExceptionStateList == null) sqlExceptionStateList = new ArrayList<>(1);
        if (!this.sqlExceptionStateList.contains(state)) this.sqlExceptionStateList.add(state);
    }

    public void removeSqlExceptionState(String state) {
        if (sqlExceptionStateList != null) this.sqlExceptionStateList.remove(state);
    }

    public String getPoolImplementClassName() {
        return this.poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (isNotBlank(poolImplementClassName)) this.poolImplementClassName = trimString(poolImplementClassName);
    }

    public boolean isEnableJmx() {
        return this.enableJmx;
    }

    public void setEnableJmx(boolean enableJmx) {
        this.enableJmx = enableJmx;
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

    public void clearAllConfigPrintExclusion() {
        this.configPrintExclusionList.clear();
    }

    public void addConfigPrintExclusion(String fieldName) {
        if (!configPrintExclusionList.contains(fieldName))
            this.configPrintExclusionList.add(fieldName);
    }

    public boolean removeConfigPrintExclusion(String fieldName) {
        return this.configPrintExclusionList.remove(fieldName);
    }

    public boolean existConfigPrintExclusion(String fieldName) {
        return this.configPrintExclusionList.contains(fieldName);
    }

    public boolean isEnableThreadLocal() {
        return enableThreadLocal;
    }

    public void setEnableThreadLocal(boolean enableThreadLocal) {
        this.enableThreadLocal = enableThreadLocal;
    }

    //****************************************************************************************************************//
    //                                     4: connection default value set methods(12)                                //
    //****************************************************************************************************************//
    public String getDefaultCatalog() {
        return this.defaultCatalog;
    }

    public void setDefaultCatalog(String defaultCatalog) {
        this.defaultCatalog = trimString(defaultCatalog);
    }

    public String getDefaultSchema() {
        return this.defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = trimString(defaultSchema);
    }

    public Boolean isDefaultReadOnly() {
        return this.defaultReadOnly;
    }

    public void setDefaultReadOnly(Boolean defaultReadOnly) {
        this.defaultReadOnly = defaultReadOnly;
    }

    public Boolean isDefaultAutoCommit() {
        return this.defaultAutoCommit;
    }

    public void setDefaultAutoCommit(Boolean defaultAutoCommit) {
        this.defaultAutoCommit = defaultAutoCommit;
    }

    public Integer getDefaultTransactionIsolationCode() {
        return this.defaultTransactionIsolationCode;
    }

    public void setDefaultTransactionIsolationCode(Integer transactionIsolationCode) {
        this.defaultTransactionIsolationCode = transactionIsolationCode;//support Informix jdbc
    }

    public String getDefaultTransactionIsolationName() {
        return this.defaultTransactionIsolationName;
    }

    public void setDefaultTransactionIsolationName(String transactionIsolationName) {
        String transactionIsolationNameTemp = trimString(transactionIsolationName);
        this.defaultTransactionIsolationCode = BeeTransactionIsolationLevels.getTransactionIsolationCode(transactionIsolationNameTemp);
        if (this.defaultTransactionIsolationCode != null) {
            defaultTransactionIsolationName = transactionIsolationNameTemp;
        } else {
            throw new BeeDataSourceConfigException("Invalid transaction isolation name:" + transactionIsolationNameTemp + ", value is one of[" + TRANS_LEVEL_CODE_LIST + "]");
        }
    }


    //****************************************************************************************************************//
    //                                     5: connection default value set Indicator methods(10)                      //
    //****************************************************************************************************************//
    public boolean isEnableDefaultOnCatalog() {
        return enableDefaultOnCatalog;
    }

    public void setEnableDefaultOnCatalog(boolean enableDefaultOnCatalog) {
        this.enableDefaultOnCatalog = enableDefaultOnCatalog;
    }

    public boolean isEnableDefaultOnSchema() {
        return enableDefaultOnSchema;
    }

    public void setEnableDefaultOnSchema(boolean enableDefaultOnSchema) {
        this.enableDefaultOnSchema = enableDefaultOnSchema;
    }

    public boolean isEnableDefaultOnReadOnly() {
        return enableDefaultOnReadOnly;
    }

    public void setEnableDefaultOnReadOnly(boolean enableDefaultOnReadOnly) {
        this.enableDefaultOnReadOnly = enableDefaultOnReadOnly;
    }

    public boolean isEnableDefaultOnAutoCommit() {
        return enableDefaultOnAutoCommit;
    }

    public void setEnableDefaultOnAutoCommit(boolean enableDefaultOnAutoCommit) {
        this.enableDefaultOnAutoCommit = enableDefaultOnAutoCommit;
    }

    public boolean isEnableDefaultOnTransactionIsolation() {
        return enableDefaultOnTransactionIsolation;
    }

    public void setEnableDefaultOnTransactionIsolation(boolean enableDefaultOnTransactionIsolation) {
        this.enableDefaultOnTransactionIsolation = enableDefaultOnTransactionIsolation;
    }

    public boolean isForceDirtyOnSchemaAfterSet() {
        return forceDirtyOnSchemaAfterSet;
    }

    public void setForceDirtyOnSchemaAfterSet(boolean forceDirtyOnSchemaAfterSet) {
        this.forceDirtyOnSchemaAfterSet = forceDirtyOnSchemaAfterSet;
    }

    public boolean isForceDirtyOnCatalogAfterSet() {
        return forceDirtyOnCatalogAfterSet;
    }

    public void setForceDirtyOnCatalogAfterSet(boolean forceDirtyOnCatalogAfterSet) {
        this.forceDirtyOnCatalogAfterSet = forceDirtyOnCatalogAfterSet;
    }

    //****************************************************************************************************************//
    //                                    6: connection factory class set methods(12)                                 //
    //****************************************************************************************************************//
    public Object getConnectionFactory() {
        return this.connectionFactory;
    }

    //connection factory
    public void setConnectionFactory(BeeConnectionFactory factory) {
        this.connectionFactory = factory;
    }

    public void setXaConnectionFactory(BeeXaConnectionFactory factory) {
        this.connectionFactory = factory;
    }

    public Class<?> getConnectionFactoryClass() {
        return this.connectionFactoryClass;
    }

    public void setConnectionFactoryClass(Class<?> connectionFactoryClass) {
        this.connectionFactoryClass = connectionFactoryClass;
    }

    public String getConnectionFactoryClassName() {
        return this.connectionFactoryClassName;
    }

    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        this.connectionFactoryClassName = trimString(connectionFactoryClassName);
    }

    public Class<? extends BeeConnectionPredicate> getEvictPredicateClass() {
        return evictPredicateClass;
    }

    public void setEvictPredicateClass(Class<? extends BeeConnectionPredicate> evictPredicateClass) {
        this.evictPredicateClass = evictPredicateClass;
    }

    public String getEvictPredicateClassName() {
        return evictPredicateClassName;
    }

    public void setEvictPredicateClassName(String evictPredicateClassName) {
        this.evictPredicateClassName = evictPredicateClassName;
    }

    public BeeConnectionPredicate getEvictPredicate() {
        return evictPredicate;
    }

    public void setEvictPredicate(BeeConnectionPredicate evictPredicate) {
        this.evictPredicate = evictPredicate;
    }

    public Class<? extends BeeJdbcLinkInfoDecoder> getJdbcLinkInfoDecoderClass() {
        return this.jdbcLinkInfoDecoderClass;
    }

    public void setJdbcLinkInfoDecoderClass(Class<? extends BeeJdbcLinkInfoDecoder> jdbcLinkInfoDecoderClass) {
        this.jdbcLinkInfoDecoderClass = jdbcLinkInfoDecoderClass;
    }

    public String getJdbcLinkInfoDecoderClassName() {
        return this.jdbcLinkInfoDecoderClassName;
    }

    public void setJdbcLinkInfoDecoderClassName(String jdbcLinkInfoDecoderClassName) {
        this.jdbcLinkInfoDecoderClassName = jdbcLinkInfoDecoderClassName;
    }

    public BeeJdbcLinkInfoDecoder getJdbcLinkInfoDecoder() {
        return jdbcLinkInfoDecoder;
    }

    public void setJdbcLinkInfoDecoder(BeeJdbcLinkInfoDecoder jdbcLinkInfoDecoder) {
        this.jdbcLinkInfoDecoder = jdbcLinkInfoDecoder;
    }

    public Object getConnectProperty(String key) {
        return this.connectProperties.get(key);
    }

    public Object removeConnectProperty(String key) {
        return this.connectProperties.remove(key);
    }

    public void addConnectProperty(String key, Object value) {
        if (isNotBlank(key) && value != null) this.connectProperties.put(key, value);
    }

    public void addConnectProperty(String connectPropertyText) {
        if (isNotBlank(connectPropertyText)) {
            for (String attribute : connectPropertyText.split("&")) {
                String[] pair = attribute.split("=");
                if (pair.length == 2) {
                    this.addConnectProperty(pair[0].trim(), pair[1].trim());
                } else {
                    pair = attribute.split(":");
                    if (pair.length == 2) {
                        this.addConnectProperty(pair[0].trim(), pair[1].trim());
                    }
                }
            }
        }
    }

    //****************************************************************************************************************//
    //                                     7: properties configuration(3)                                             //
    //****************************************************************************************************************//
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
                throw new IllegalArgumentException("Target object is a valid configuration file:" + filename);
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

        //1:load configuration item values from outside properties
        Map<String, String> setValueMap;
        synchronized (configProperties) {/* synchronization mode */
            Set<Map.Entry<Object, Object>> entrySet = configProperties.entrySet();
            setValueMap = new HashMap<>(entrySet.size());
            for (Map.Entry<Object, Object> entry : entrySet) {
                setValueMap.put((String) entry.getKey(), (String) entry.getValue());
            }
        }

        //2: exclude some special keys in setValueMap
        String connectPropertiesText = setValueMap.remove(CONFIG_CONNECT_PROP);//remove item if exists in properties file before injection
        String connectPropertiesSize = setValueMap.remove(CONFIG_CONNECT_PROP_SIZE);//remove item if exists in properties file before injection
        String sqlExceptionCode = setValueMap.remove(CONFIG_SQL_EXCEPTION_CODE);//remove item if exists in properties file before injection
        String sqlExceptionState = setValueMap.remove(CONFIG_SQL_EXCEPTION_STATE);//remove item if exists in properties file before injection
        String exclusionListText = setValueMap.remove(CONFIG_CONFIG_PRINT_EXCLUSION_LIST);
        try {
            setPropertiesValue(this, setValueMap);
        } catch (BeanException e) {
            throw new BeeDataSourceConfigException(e.getMessage(), e);
        }

        //3:try to find 'connectProperties' config value and put to ds config object
        this.addConnectProperty(connectPropertiesText);
        if (isNotBlank(connectPropertiesSize)) {
            int size = Integer.parseInt(connectPropertiesSize.trim());
            for (int i = 1; i <= size; i++)//properties index begin with 1
                this.addConnectProperty(getPropertyValue(setValueMap, CONFIG_CONNECT_PROP_KEY_PREFIX + i));
        }

        //4: add error codes if not null and not empty
        if (isNotBlank(sqlExceptionCode)) {
            for (String code : sqlExceptionCode.trim().split(",")) {
                try {
                    this.addSqlExceptionCode(Integer.parseInt(code));
                } catch (NumberFormatException e) {
                    throw new BeeDataSourceConfigException(code + " is not a valid SQLException error code");
                }
            }
        }

        //5: add sql states if not null and not empty
        if (isNotBlank(sqlExceptionState)) {
            for (String state : sqlExceptionState.trim().split(",")) {
                this.addSqlExceptionState(state);
            }
        }

        //6:try to load exclusion list on config print
        if (isNotBlank(exclusionListText)) {
            this.clearAllConfigPrintExclusion();//remove existed exclusion
            for (String exclusion : exclusionListText.trim().split(",")) {
                this.addConfigPrintExclusion(exclusion);
            }
        }
    }

    //****************************************************************************************************************//
    //                                    8: configuration check and connection factory create methods(4)             //
    //****************************************************************************************************************//

    /**
     * Check on this configuration,return its copy if success
     *
     * @return a copy of current configuration
     * @throws BeeDataSourceConfigException when check configuration failed
     * @throws SQLException                 when failed to load a driver with a configured class name or other check on a driver
     */
    public BeeDataSourceConfig check() throws SQLException {
        if (initialSize > maxActive)
            throw new BeeDataSourceConfigException("initialSize must not be greater than maxActive");
        if (!aliveTestSql.toUpperCase(Locale.US).startsWith("SELECT ")) {
            //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end
            throw new BeeDataSourceConfigException("Alive test sql must be start with 'select '");
        }

        Object connectionFactory = createConnectionFactory();
        BeeConnectionPredicate predicate = this.createConnectionEvictPredicate();

        BeeDataSourceConfig checkedConfig = new BeeDataSourceConfig();
        copyTo(checkedConfig);
        if (this.connectionFactory != null || connectionFactoryClass != null || isNotBlank(connectionFactoryClassName)) {
            if (isNotBlank(this.username) || isNotBlank(this.password) || isNotBlank(this.jdbcUrl) || isNotBlank(driverClassName)) {
                CommonLog.info("BeeCP({})configured jdbc link info abandoned according that a connection factory has been existed", "...");
                checkedConfig.username = null;
                checkedConfig.password = null;
                checkedConfig.jdbcUrl = null;
                checkedConfig.driverClassName = null;
            }
        }

        //set some factories to config
        this.connectionFactory = connectionFactory;
        checkedConfig.connectionFactory = connectionFactory;
        checkedConfig.evictPredicate = predicate;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "FastPool-" + PoolNameIndex.getAndIncrement();
        if (checkedConfig.printConfigInfo) printConfiguration(checkedConfig);

        return checkedConfig;
    }

    //copy configuration info to other from local
    void copyTo(BeeDataSourceConfig config) {
        String fieldName = null;
        try {
            for (Field field : BeeDataSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                fieldName = field.getName();
                switch (fieldName) {
                    case CONFIG_CONFIG_PRINT_EXCLUSION_LIST: //copy 'exclusionConfigPrintList'
                        if (configPrintExclusionList.isEmpty())
                            config.configPrintExclusionList.clear();
                        else
                            config.configPrintExclusionList.addAll(configPrintExclusionList);
                        break;
                    case CONFIG_CONNECT_PROP: //copy 'connectProperties'
                        config.connectProperties.putAll(connectProperties);
                        break;
                    case CONFIG_SQL_EXCEPTION_CODE: //copy 'sqlExceptionCodeList'
                        if (this.sqlExceptionCodeList != null && !sqlExceptionCodeList.isEmpty())
                            config.sqlExceptionCodeList = new ArrayList<>(sqlExceptionCodeList);
                        break;
                    case CONFIG_SQL_EXCEPTION_STATE: //copy 'sqlExceptionStateList'
                        if (this.sqlExceptionStateList != null && !sqlExceptionStateList.isEmpty())
                            config.sqlExceptionStateList = new ArrayList<>(sqlExceptionStateList);
                        break;
                    default: //other config items
                        field.set(config, field.get(this));
                }
            }
        } catch (Exception e) {
            throw new BeeDataSourceConfigException("Failed to copy field[" + fieldName + "]", e);
        }
    }

    //create BeeJdbcLinkInfoDecoder instance
    private BeeJdbcLinkInfoDecoder createJdbcLinkInfoDecoder() {
        if (jdbcLinkInfoDecoder != null) return this.jdbcLinkInfoDecoder;

        //step2: create link info decoder
        if (jdbcLinkInfoDecoderClass != null || isNotBlank(jdbcLinkInfoDecoderClassName)) {
            Class<?> decoderClass = null;
            try {
                decoderClass = jdbcLinkInfoDecoderClass != null ? jdbcLinkInfoDecoderClass : Class.forName(jdbcLinkInfoDecoderClassName);
                return (BeeJdbcLinkInfoDecoder) createClassInstance(decoderClass, BeeJdbcLinkInfoDecoder.class, "jdbc link info decoder");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create jdbc link info decoder with class[" + jdbcLinkInfoDecoderClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create sql exception predication with class[" + decoderClass + "]", e);
            }
        }
        return null;
    }

    //create Connection factory
    private Object createConnectionFactory() throws SQLException {
        //step1:if exists object factory,then return it
        if (this.connectionFactory != null) return this.connectionFactory;

        //step2:create connection factory with driver
        Properties jdbcLinkInfoProperties = getJdbcLinkInfoProperties();
        BeeJdbcLinkInfoDecoder jdbcLinkInfoDecoder = this.createJdbcLinkInfoDecoder();
        if (this.connectionFactoryClass == null && isBlank(this.connectionFactoryClassName)) {
            //step2.1: prepare jdbc url
            String url = jdbcLinkInfoProperties.getProperty("url");
            if (isBlank(url)) throw new BeeDataSourceConfigException("jdbcUrl can't be null");
            if (jdbcLinkInfoDecoder != null) url = jdbcLinkInfoDecoder.decodeUrl(url);//decode url

            //step2.2: find a matched driver
            Driver driver;
            if (isNotBlank(this.driverClassName)) {
                driver = loadDriver(this.driverClassName);
                if (!driver.acceptsURL(url))
                    throw new BeeDataSourceConfigException("jdbcUrl(" + url + ")can not match configured driver[" + driverClassName + "]");
            } else {
                driver = DriverManager.getDriver(url);//try to get a matched driver with url,if not found,a SQLException will be thrown from driverManager
            }

            //step2.3: get username and password
            String username = jdbcLinkInfoProperties.getProperty("user");
            String password = jdbcLinkInfoProperties.getProperty("password");

            //step2.4: decode username and password
            if (jdbcLinkInfoDecoder != null) {
                if (isNotBlank(username))
                    username = jdbcLinkInfoDecoder.decodeUsername(username);
                if (isNotBlank(password))
                    password = jdbcLinkInfoDecoder.decodePassword(password);
            }

            //step2.5: make a copy from connect properties
            Properties localConnectProperties = new Properties();
            localConnectProperties.putAll(this.connectProperties);

            //2.6: set username and password to local connectProperties
            if (isNotBlank(username)) {
                localConnectProperties.setProperty("user", username);
                if (isNotBlank(password)) localConnectProperties.setProperty("password", password);
            }

            //step2.7: create a new connection factory with a driver and jdbc link info properties
            return new ConnectionFactoryByDriver(url, driver, localConnectProperties);
        } else {//step3:create connection factory with connection factory class
            Class<?> conFactClass = null;
            try {
                //3.1: load connection factory class with class name
                conFactClass = this.connectionFactoryClass != null ? this.connectionFactoryClass : Class.forName(this.connectionFactoryClassName);

                //3.2: check connection factory class
                Class<?>[] parentClasses = {BeeConnectionFactory.class, BeeXaConnectionFactory.class, DataSource.class, XADataSource.class};

                //3.3: create connection factory instance
                Object factory = createClassInstance(conFactClass, parentClasses, "connection factory");

                //3.4: create a copy on local connectProperties
                Map<String, Object> localConnectProperties = new HashMap<>(this.connectProperties);//copy

                //3.5: set jdbc link info
                String url = jdbcLinkInfoProperties.getProperty("url");
                String username = jdbcLinkInfoProperties.getProperty("user");
                String password = jdbcLinkInfoProperties.getProperty("password");

                //3.6: decode jdbc link info
                if (jdbcLinkInfoDecoder != null) {//then execute the decoder
                    if (isNotBlank(url))
                        url = jdbcLinkInfoDecoder.decodeUrl(url);
                    if (isNotBlank(username))
                        username = jdbcLinkInfoDecoder.decodeUsername(username);
                    if (isNotBlank(password))
                        password = jdbcLinkInfoDecoder.decodePassword(password);
                }

                //3.7: reset jdbc url to the target map
                if (isNotBlank(url)) {//reset url to properties map with three key names
                    localConnectProperties.put("url", url);
                    localConnectProperties.put("URL", url);
                    localConnectProperties.put("jdbcUrl", url);
                }

                //3.8: set username and password to local connectProperties
                if (isNotBlank(username)) {
                    localConnectProperties.put("user", username);
                    if (isNotBlank(password))
                        localConnectProperties.put("password", password);
                }

                //3.9: inject properties to connection factory or dataSource
                setPropertiesValue(factory, localConnectProperties);

                //3.10: return RawConnectionFactory or RawXaConnectionFactory
                if (factory instanceof BeeConnectionFactory || factory instanceof BeeXaConnectionFactory) {
                    return factory;
                } else if (factory instanceof XADataSource) {
                    return new XaConnectionFactoryByDriverDs((XADataSource) factory, username, password);
                } else {//here,factory must be a datasource(only support 4 types,because that factory class type check before creation)
                    return new ConnectionFactoryByDriverDs((DataSource) factory, username, password);
                }
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Not found connection factory class[" + conFactClass + "]", e);
            } catch (BeeDataSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create connection factory with class[" + conFactClass + "]", e);
            }
        }
    }

    //look up jdbc link info
    private Properties getJdbcLinkInfoProperties() {
        String url = this.jdbcUrl;//note:jdbc url is a base info
        String username = this.username;
        String password = this.password;

        if (isBlank(url)) {
            url = (String) this.connectProperties.get("url");
            if (isBlank(url)) url = (String) connectProperties.get("URL");
            if (isBlank(url)) url = (String) connectProperties.get("jdbcUrl");
            if (isNotBlank(url)) {//url found from connectProperties
                username = (String) connectProperties.get("user");
                password = (String) connectProperties.get("password");
            } else {
                url = System.getProperty("beecp.url");
                if (isBlank(url)) url = System.getProperty("beecp.URL");
                if (isBlank(url)) url = System.getProperty("beecp.jdbcUrl");
                if (isNotBlank(url)) {//url found from system properties
                    username = System.getProperty("beecp.user");
                    password = System.getProperty("beecp.password");
                }
            }
        }

        Properties jdbcLinkInfoProperties = new Properties();
        if (isNotBlank(url)) jdbcLinkInfoProperties.put("url", url);
        if (isNotBlank(username)) jdbcLinkInfoProperties.put("user", username);
        if (isNotBlank(password)) jdbcLinkInfoProperties.put("password", password);
        return jdbcLinkInfoProperties;
    }

    //create Thread factory
    private BeeConnectionPredicate createConnectionEvictPredicate() throws BeeDataSourceConfigException {
        //step1:if exists predication,then return it
        if (this.evictPredicate != null) return this.evictPredicate;

        //step2: create SQLExceptionPredication
        if (evictPredicateClass != null || isNotBlank(evictPredicateClassName)) {
            Class<?> predicationClass = null;
            try {
                predicationClass = evictPredicateClass != null ? evictPredicateClass : Class.forName(evictPredicateClassName);
                return (BeeConnectionPredicate) createClassInstance(predicationClass, BeeConnectionPredicate.class, "sql exception predicate");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Not found sql exception predicate class[" + evictPredicateClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create sql exception predicate with class[" + predicationClass + "]", e);
            }
        }

        return null;
    }

    //print check passed configuration
    private void printConfiguration(BeeDataSourceConfig checkedConfig) {
        String poolName = checkedConfig.poolName;
        CommonLog.info("................................................BeeCP({})configuration[start]................................................", poolName);
        try {
            for (Field field : BeeDataSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                String fieldName = field.getName();
                boolean infoPrint = !checkedConfig.configPrintExclusionList.contains(fieldName);
                switch (fieldName) {
                    case CONFIG_CONFIG_PRINT_EXCLUSION_LIST: //copy 'exclusionConfigPrintList'
                        break;
                    case CONFIG_CONNECT_PROP: //copy 'connectProperties'
                        if (!connectProperties.isEmpty()) {
                            if (infoPrint) {
                                for (Map.Entry<String, Object> entry : checkedConfig.connectProperties.entrySet())
                                    CommonLog.info("BeeCP({}).connectProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            } else {
                                for (Map.Entry<String, Object> entry : checkedConfig.connectProperties.entrySet())
                                    CommonLog.debug("BeeCP({}).connectProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            }
                        }
                        break;
                    default:
                        if (infoPrint)
                            CommonLog.info("BeeCP({}).{}={}", poolName, fieldName, field.get(checkedConfig));
                        else
                            CommonLog.debug("BeeCP({}).{}={}", poolName, fieldName, field.get(checkedConfig));
                }
            }
        } catch (Throwable e) {
            CommonLog.warn("BeeCP({})failed to print configuration", poolName, e);
        }
        CommonLog.info("................................................BeeCP({})configuration[end]................................................", poolName);
    }
}

