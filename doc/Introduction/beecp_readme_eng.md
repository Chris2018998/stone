[🏠](../../README.md) [English](beecp_readme_eng.md)|[中文](beecp_readme_cn.md)

![](https://img.shields.io/badge/Java-8+-green.svg)
![](https://img.shields.io/maven-central/v/io.github.chris2018998/stone?logo=apache-maven)
[![License](https://img.shields.io/github/license/Chris2018998/stone?color=4D7A97&logo=apache)](https://github.com/Chris2018998/stone/blob/main/LICENSE)

BeeCP is a fast JDBC connection pool has techology features: caching single connection, not-moving waiting, fixed length array.

##
✨**Highlight Features**

* Support clearing and reinitalizing
* Support properties file configuration
* Support virtual thread applications
* Provide method to interrupt blocking
* Provide interfaces to be customizated
* [Provide starter and web monitor](https://github.com/Chris2018998/beecp-starter)

![image](https://github.com/user-attachments/assets/d2753c33-e671-4d79-92e5-cfb4cae281e0)<br/>

![image](https://github.com/user-attachments/assets/31c37580-9cec-42fa-b56f-3421052ab3b8)

##
📊**Performance of Pools**

JMH Performance tested with HikariCP-benchmark.

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>

##
🍒***Compare to HikariCP***

| Item                                                                | HikariCP                | BeeCP                    |
|-------------------------------------------------------------------  |-------------------------|--------------------------|
| Number of connection in threadlocal                                 | >=1                                      | =1                                          |
| Type of container store connections                                 | CopyOnWriteArrayList                     | An array of fixed length                    |
| Transfer queue/wait queue                                           | SynchronousQueue                         | ConcurrentLinkedQueue                       |
| Asyn way to create connections                                      | Thread pool                              | Single thread                               |
| Support concurrency creation of connections                         | Not Support                              | Support                                     |
| Support clearing and reinitialization                               | Not Support                              | Support                                     |
| Provide method to interrupt blocking                                | Not Provide                              | Provide                                     |
| Provide interfaces to be customizated                               | Only exceptionOverride                   | Provide                                     |
| Provide configuration for exception code and sql state              | Not Support                              | Provide                                     |
| Support threadLocal-cache disable                                   | Not Support                              | Support                                     |
| Support XADataSource                                                | Not Support                              | Support                                     |
| Support switch between log print and not print                      | Not Support                              | Support                                     |
| Support force reset on schema,catalog (transaction)                 | Not Support                              | Support                                     |
| Support reading default from first connection if not configuered(5) | Not Support                              | Support                                     |
| Properties of data source                                           | java.util.Properties                     | java.util.Map<String,Object>                |
| Statement Cache                                                     | Depends driver                           | Depends driver                              |
| minimumIdle of HikariCP                                             | setMinimumIdle(int)                      | Not Provide(can be regarded as zero)        |
| maximumPoolSize of HikariCP                                         | setMaximumPoolSize(int)                  | setMaxActive(int)                           |
| connectionTimeout of HikariCP                                       | setConnectionTimeout(long)               | setMaxWait(long)                            |
| idleTimeout of HikariCP                                             | setIdleTimeout(long)                     | setIdleTimeout(long)                        |
| keepaliveTime of HikariCP                                           | setKeepaliveTime(long)                   | setTimerCheckInterval(long)                 |
| maxLifetime of HikariCP                                             | setMaxLifetime(long)                     | Not Provide(idleTimeout and holdTimeout)    |
| connectionTestQuery of HikariCP                                     | setConnectionTestQuery(String)           | setAliveTestSql(String)                     |
| validationTimeout of HikariCP                                       | setValidationTimeout(long)               | setAliveTestTimeout(long)                   |

_[**HikariCP**](https://github.com/brettwooldridge/HikariCP) is an excellent open source project and widely used in the Java world, it is developed by Brettwooldridge, a senior JDBC expert of United States_

## 
⏰**DB Down Test**

As famous [5 seconds timeout test on pools](https://github.com/brettwooldridge/HikariCP/wiki/Bad-Behavior:-Handling-Database-Down), Brettwooldridge(the author of HikariCP) did a test with four pools to verify timeout reactivity on scenario of database down, but only HikariCP pool could respond within five seconds, so we do the same test with BeeCP. [View the test source code](../beecp/test/src/main/java/org/stone/beecp/other/DbDownTest.java).


|     Requirement          | Settig                                                    |  Remark                                                                                             |
|--------------------------|----------------------------------------------------------------|----------------------------------------------------------------------------------------------------- |
| database                 | mysql-8.4.3                                                    |                                                                                                      |
| driver                   | mysql-connector-j-8.3.0.jar                                    |                                                                                                      |
| url                      | jdbc:mysql://hostIP/test?connectTimeout=50&socketTimeout=100   |the connectTimeout is socket level parameter of mysql jdbc driver                                     |
| timeout                  | **5000** milliseconds                                          |HikariConfig.setConnectionTimeout(5000); BeeDataSourceConfig.setMaxWait(5000);                        |
| Pool version             | HikariCP-6.2.1, stone-1.4.6                                    |                                                                                                      |
| Java version             | Java-22.0.2                                                    |                                                                                                      |

![image](https://github.com/user-attachments/assets/4cca47e0-04d2-4792-a070-1bf9f1bd0306)

**Retest with larger value(18000ms)**
|     Requirement          | Settig                                                         |  Remark                                                                                             |
|--------------------------|----------------------------------------------------------------|---------------------------------------------------------------------------------------------------- |
| timeout                  | **18000** milliseconds                                         |HikariConfig.setConnectionTimeout(18000); BeeDataSourceConfig.setMaxWait(18000);                     |
| Others                   | No Change                                                      |                                                                                                     |
 
![image](https://github.com/user-attachments/assets/4e0d70b4-e68a-4b28-b1c8-bfb0a949e401)


*^-^ if set **Long.MAX_VALUE** to timeout, what will be happen?*

**Pool Grading**

| Pool	        |Grade   | Reason                                      |
|--------------|--------|---------------------------------------------|
| HikariCP     | A      |Properly handles connection timeouts.        |
| BeeCP        | A+     |Socket level response.                       |


## 
✈️**Operation on closed Statement**

I believe many people have known that there exists dependency relationship between JDBC connection, preparedStatement, and resultSet. If close owner object, its opened objects will automatically be closed, however, there is an exception to this, let us do a test to verify it.[View the test source code](../beecp/test/src/main/java/org/stone/beecp/other/MysqlClosedPreparedStatementTest.java).

![image](https://github.com/user-attachments/assets/f75d5684-ff4f-4ad9-b88e-f453e833ea69)

*^-^ It is an issue? how to resolve it?*

## 
👉**How To Use It**

BeeCP provide datasource implementation wrap pool instance and its use is like other pools.

* Sample one(*Traditional*)

```java

//step1: create datasource
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.setDriverClassName("com.mysql.cj.jdbc.Driver");
config.setJdbcUrl("jdbc:mysql://localhost/test");
config.setUsername("root");
config.setPassword("root");
BeeDataSource ds = new BeeDataSource(config);

//step2：get connection
try(Connection con = ds.getConnection()){
  //...... your code
}
```

* Sample Second(*Springboot*)

```java
@Configuration
public class DataSourceConfiguration{

  @Bean
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource ds1(){
     return new BeeDataSource();
  }

  @Bean
  public DataSource ds2(){
    BeeDataSourceConfig config = new BeeDataSourceConfig();
    config.setJdbcUrl("jdbc:mysql://localhost:3306/test");
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setUsername("root");
    config.setPassword("root");
    //......you can set more properties 
    return new BeeDataSource(config);
  }
}
```
* Sample Third([*beecp-starter*](https://github.com/Chris2018998/beecp-starter))

*_application.properties_*
  
```properties
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.jdbcUrl=jdbc:mysql://localhost:3306/test
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.initialSize=1
spring.datasource.maxActive=10
spring.datasource.maxWait=30000
......
```

##
🔡**List Of Configuration Properties**

BeeCP provide a configuration object, which defines some properties to be set.

| Property name                   | Description                                                                          | Default value                                       |
|---------------------------------|--------------------------------------------------------------------------------------|-----------------------------------------------------|
| username                        | jdbc username link to database                                                       | none                                                |
| password                        | jdbc password link to database                                                       | none                                                |
| jdbcUrl                         | jdbc url link to database                                                            | none                                                |
| driverClassName                 | jdbc driver class name                                                               | none                                                |
| poolName	                      | If not set, a name generated for it                                                  | none                                                |
| fairMode                        | Connection getting mode applied on semaphore and transfer                            | false（unfair mode）                                | 
| initialSize                     | Creation size of connections when pool initializes                                   | 0                                                   |
| maxActive                       | Maximum of connections in pool                                                       | Math.min(Math.max(10, NCPU), 50)                    | 
| borrowSemaphoreSize             | Max permit size of semaphore for conneciton getting                                  | min(maxActive/2,CPU size）                          |
| defaultAutoCommit               | Connection.setAutoComit(defaultAutoCommit),if not set, read it from first connection | none                                                |
| defaultTransactionIsolationCode | Connection.setTransactionIsolation(defaultTransactionIsolationCode), if not set, read it from first connection| none                        |
| defaultCatalog                  | Connection.setCatalog(defaultCatalog), if not set, read it from first connection      | none                                                |
| defaultSchema                   | Connection.setSchema(defaultSchema), if not set, read it from first connection        | none                                                |
| defaultReadOnly                 | Connection.setReadOnly(defaultReadOnly), if not set,read it from first connection     | none                                                |
| maxWait                         | Max wait time in pool to get a connection for borrower(ms)                            | 8000                                                |
| idleTimeout                     | Max idle time of connections in pool(ms)                                              | 18000                                               |  
| holdTimeout                     | Max inactive time of borrowed connections(ms)                                         | 0(no timeout)                                       |  
| aliveTestSql                    | An test sql to check alive on borrowed connections                                    | SELECT 1                                            |  
| aliveTestTimeout                | Max wait time to get alive test result from borrowed connections(seconds)             | 3                                                   |  
| aliveAssumeTime                 | A threshold time of alive test on borrowed connections, if gap time(Last active time **to** Borrowed time) is less than this value, connections need not be tested(ms)| 500 |  
| forceRecycleBorrowedOnClose     | An indicator to recycle borrowed connections and make them return to pool when pool shutdown| false                                        |
| parkTimeForRetry                | A park time to wait borrowed connections return to pool(ms)                             | 3000                                             |             
| timerCheckInterval              | An interval time to scans out timeout connections(idle timeout and hold timeout) (ms)   | 18000                                            |
| forceDirtyOnSchemaAfterSet      | An indicator of force dirty on schema property to support to be reset under transaction, for example:PG driver  | false                     |
| forceDirtyOnCatalogAfterSet     | An indicator of force dirty on catalog property to support to be reset under transaction, for example:PG driver | false                     |
| enableThreadLocal               | A indicator to enable or disable threadlocal in pool（false to support virtual threads) | true                                              | 
| enableJmx                       | A indicator to enable or disable pool registeration to JMX                              | false                                            | 
| printConfigInfo                 | A indicator to print configuration info by log when pool initializes                    | false                                            | 
| printRuntimeLog                 | A indicator to print pool working logs, also pool provide method to support switch between print or not print| false                            | 
| **connectionFactory**               | Connection factory instance                                                         | none                                              |
| **connectionFactoryClass**          | Connection factory class, a constructor without parameters is required              | none                                              |
| **connectionFactoryClassName**      | Connection factory class name, a constructor without parameters is required         | none                                              |
| **evictPredicate**                  | Predicate instance                                                                  | none                                              |
| **evictPredicateClass**             | Predicate class, a constructor without parameters is required                       | none                                              |
| **evictPredicateClassName**         | Predicate class name, a constructor without parameters is required                  | none                                              |
| **jdbcLinkInfoDecoder**             | Decoder instance of jdbc link info                                                  | none                                              |
| **jdbcLinkInfoDecoderClass**        | Decoder class of jdbc link info, a constructor without parameters is required       | none                                              |
| **jdbcLinkInfoDecoderClassName**    | Decoder class name of jdbc link info, a constructor without parameters is required  | none                                              |

***Object type properties**，effective order：instance > class > class name

##
📝**Configuration Loading**

BeeCP supports loading configuration properties from properties files and properties object(*java.util.Properties*), a reference example is below

*_config.properties_*

```properties
username=root
password=root
jdbcUrl=jdbc:mysql://localhost/test
driverClassName=com.mysql.cj.jdbc.Driver

initial-size=1
max-active=10
.......
```
*_*Supports three format of Property name: camel hump, middle line, underline_*

```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
// load from properties file
config.loadFromPropertiesFile("d:\beecp\config.properties");

// load from properties
// Properties configProperties = new Properties();
// configProperties.setProperty("username","root");
// configProperties.setProperty("password","password");
// configProperties.setProperty("jdbcUrl","jdbc:mysql://localhost/test");
// configProperties.setProperty("driverClassName","com.mysql.cj.jdbc.Driver");
// configProperties.setProperty("initial-size","1");
// configProperties.setProperty("max-active","10");
// config.loadFromProperties(configProperties);
```

##
⚙**Driver Parameters Setting**

BeeCP pool uses a driver, a datasource or a connection factory to create connections,whose work may depend some parameters need be set from outside. BeeCP has defined two methods[**addConnectProperty(String,Object); addConnectProperty(String)**] in its configuration object(*org.stone.beecp.BeeDataSourceConfig*) to add those parameters injected to 
driver/datasource/connection factory during pool initializing. Three segment blocks are blow for reference.

***Reference 1**(java code to set them)*
```java
 BeeDataSourceConfig config = new BeeDataSourceConfig();
 config.addConnectProperty("cachePrepStmts", "true");
 config.addConnectProperty("prepStmtCacheSize", "250");
 config.addConnectProperty("prepStmtCacheSqlLimit", "2048");

 //or
 config.addConnectProperty("cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048");

 //or 
 config.addConnectProperty("cachePrepStmts:true&prepStmtCacheSize:250&prepStmtCacheSqlLimit:2048");
```

***Reference 2**(properties file to configure them)*

```properties
connectProperties=cachePrepStmts=true&prepStmtCacheSize=50
```
***Reference 3**(properties file to configure them，recommand this way if multiple parameters)*

```properties
connectProperties.size=2
connectProperties.1=prepStmtCacheSize=50
connectProperties.2=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true
```

##
📤**Connection Eviction**

 BeeCP provides three ways to evict connections from pool: *method call(1)* and *configuration match check(2,3)* and *predicate check(4)*

1. Eviction by calling **abort** method of connections.
```java
Connection con = beeDs.getConneciton();
con.abort(null);
```

2. Eviction by **checking code of sql-exception** thrown from borrowed connections.
```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.addSqlExceptionCode(500150);
```

3. Eviction by **checking state of sql-exception** thrown from borrowed connections.
```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.addSqlExceptionState("57P01");
```
4. Eviction by **checking sql-exception with predicate** thrown from borrowed connections.
```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.setEvictPredicateClassName("org.stone.beecp.objects.MockEvictConnectionPredicate");
```
5. Properties file 
```properties
sqlExceptionCodeList=500150,2399,1105
sqlExceptionStateList=0A000,57P01,57P02,57P03,01002,JZ0C0,JZ0C1
evictPredicateClassName=org.stone.beecp.objects.MockEvictConnectionPredicate
```
* Priority order: Predicate check > error code check > sql state check

##
🛤️**Interrupt blocking**

Maybe database overhead too heavy, or maybe network issues, or other reasons, sometime the creation of connections blocking in JDBC Pools, BeeDatasource provides a method to attempt to interrupt blocking.

```java
BeeConnectionPoolMonitorVo vo = beeDs.getPoolMonitorVo();
System.out.println("Count of creation in processing:"+vo.getCreatingCount());
System.out.println("Count of creation timeout is:"+vo.getCreatingTimeoutCount());

//*only interrupt timeout creation
beeDs.interruptConnectionCreating(true);
```

##
🛒**Clean and Reinitialization**

BeeCP provides two clear methods on the data source (BeeDataSource) to clean up the connections created in the pool and restore the pool to its initial state,not accept external requests during clean

* ```clear(boolean forceCloseUsing);//forceCloseUsing is true,then recyle borrowed conenction by force ```

* ```clear(boolean forceCloseUsing, BeeDataSourceConfig config);//forceCloseUsing is true,then recyle borrowed conenction by force；then reinitiaize pool with new configuration```

*_Interrupt them if connection creation exist druing clean process;let waiters to exit waiting for ending request of connection getting_


##
🏭**Factory customization**

Beecp provides factory interfaces (BeeConnectFactory, BeeXaConnectFactory) for custom implementation of connection
creation, and there are four methods on the BeeDataSourceConfig object (setConnectFactory, setXaConnectFactory,
setConnectFactoryClass, setConnectFactoryClassName) to set the factory object, factory class, and factory class name
respectively. The order of effective selection is: factory object>factory class>factory class name,below is a reference
example

```java
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import org.stone.beecp.BeeConnectionFactory;

public class MyConnectionFactory implements BeeConnectionFactory {
    private final String url;
    private final Driver driver;
    private final Properties connectInfo;

    public MyConnectionFactory(String url, Properties connectInfo, Driver driver) {
        this.url = url;
        this.driver = driver;
        this.connectInfo = connectInfo;
    }

    public Connection create() throws SQLException {
        return driver.connect(url, connectInfo);
    }
}


public class MyConnectionDemo {
    public static void main(String[] args) throws SQLException {
        final String url = "jdbc:mysql://localhost:3306/test";
        final Driver myDriver = DriverManager.getDriver(url);
        final Properties connectInfo = new Properties();
        connectInfo.put("user","root");
        connectInfo.put("password","root");

        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactory(new MyConnectionFactory(url, connectInfo, myDriver));
        BeeDataSource ds = new BeeDataSource(config);

        try (Connection con = ds.getConnection()) {
            //put your code here
        }
    }
}

```

_Reminder: If both the connection factory and four basic parameters (driver, URL, user, password) are set
simultaneously, the connection factory will be prioritized for use._



