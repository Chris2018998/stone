[English](README.md)|[中文](README_CN.md)

![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![](https://img.shields.io/github/license/Chris2018998/BeeCP)

BeeCP is a lightweight JDBC connection pool,its Jar file only 133KB and its techology highlights: caching single connection, non moving waiting, fixed length array

---
Java7+

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>4.1.5</version>
</dependency>
```

Java6(deprecated)

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.10</version>
</dependency>
```                                

---
**Highlight Features**

* Provide interruption for blocking in pool
* Support Pool clean and pool reinitalize
* Support configuration properties file
* Provide interfaces for customization
* Support virtual thread applications
* [Provide web monitor](https://github.com/Chris2018998/beecp-starter)

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)
![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

_Reminder: If your project is built on springboot framework and also you are interested at beecp or already using it,we recommend [beecp starter](https://github.com/Chris2018998/beecp-starter) to
you._

**JMH Performance**

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>


***Compare to HikariCP***

| item                                           | HikariCP             | BeeCP                 |
|------------------------------------------------|----------------------|-----------------------|
| Connection caching size in threadlocal         | Multiple             | Single                |
| Container type to store pooled connections     | CopyOnWriteArrayList | A fixed length array  |
| Transfer queue/wait queue                      | SynchronousQueue     | ConcurrentLinkedQueue |
| Way to add connection to pool                  | Thread pool          | Single thread         |
| Connection creation by concurrency             | Not Support          | Support               |
| Pool clean and pool reinitialize               | Not Support          | Support               |
| Provide interruption methods for blocking      | Not Provide          | Provide               |
| Provide connection factory interface           | Not Support          | Not Support           |
| Disable threadLocal to support virtual thread  | Not Support          | Support               |
| Support XADataSource                           | Not Support          | Support               |

_[**HikariCP**](https://github.com/brettwooldridge/HikariCP) is an excellent open source project and widely used in the Java world,it is developed by Brettwooldridge,a senior JDBC expert of United States_

--- 
**How to use it**

Its usage is generally similar to popular connection pools,and some reference source codes in followed chapters 

--- 
**Configuration properties**

BeeCP woring parameters are from its configuration object(BeeDataSourceConfig),below is a list of properites,which can be confiured by their set methods

| property name                   | description                                                            | default value             |
|---------------------------------|------------------------------------------------------------------------|---------------------------|
| username                        | user name of db                                                        | blank                     |
| password                        | user password of db                                                    | blank                     |
| jdbcUrl                         | link url to db                                                         | blank                     |
| driverClassName                 | jdbc driver class name                                                 | blank                     |
| poolName	                  | pool name,if not set,a generated name will be assigned to it           | blank                     |
| fairMode                        | a mode to get connections from pool                                    | false（unfair mode）       | 
| initialSize                     | creation size of connecitons during pool initialization                | 0                         |
| maxActive                       | max size of connections in pool                                        | 10                        | 
| borrowSemaphoreSize             | max permit size of semaphore for conneciton getting                    | min(maxActive/2,CPU size） |
| defaultAutoCommit               | Connection.setAutoComit(defaultAutoCommit)                             | blank                     |
| defaultTransactionIsolationCode | Connection.setTransactionIsolation(defaultTransactionIsolationCode)    | blank                     |
| defaultCatalog                  | Connection.setCatalog(defaultCatalog)                                  | blank                     |
| defaultSchema                   | Connection.setSchema(defaultSchema)                                    | blank                     |
| defaultReadOnly                 | Connection.setReadOnly(defaultReadOnly)                                | blank                     |
| maxWait                         | max wait time in pool to get connection(ms)                            | 8000                      |
| idleTimeout                     | max idle time of connecitons in pool (ms)                              | 18000                     |  
| holdTimeout                     | max inactive time of borrowed connections(ms)                          | 0                         |  
| aliveTestSql                    | alive test sql                                                         | SELECT 1                  |  
| aliveTestTimeout                | max wait time to get alive check result(seconds)                       | 3                         |  
| aliveAssumeTime                 | a hreshold time to do alive check on borrowed connections,assume alive if less,otherwise check(ms)| 500                       |  
| forceCloseUsingOnClear          | indicator to recyle borrowed connecton by force when pool clean       | false                     |
| parkTimeForRetry                | timed wait for borrowed connections to return to pool and close them(ms)   | 3000                      |             
| timerCheckInterval              | a iterval time for pool to scan idle-timeout conencitons (ms)              | 18000                     |
| forceDirtyOnSchemaAfterSet      | force reset flag for schema property when conneciton close(can used in app of PG) | false                     |
| forceDirtyOnCatalogAfterSet     | force reset flag for catlog property when conneciton close(can used in app of PG) | false                     |
| enableThreadLocal               | an indicator to enable/disable threadlocal in pool（false to support VT)    |  true                      | 
| enableJmx                       | enable indicator to support Jmx                                        | false                     | 
| printConfigInfo                 | indicator to print configuration items by log when pool initialize     | false                     | 
| printRuntimeLog                 | indicator to print runtime logs of pool                                | false                     | 
| **connectionFactory**               | connection factory instance                                            | blank                     |
| **connectionFactoryClass**          | connection factory class                                               | blank                     |
| **connectionFactoryClassName**      | connection factory class name                                          | blank                     |
| **evictPredicate**                  | predicate instance                                                     | blank                     |
| **evictPredicateClass**             | predicate class                                                        | blank                     |
| **evictPredicateClassName**         | predicate class name                                                   | blank                     |
| **jdbcLinkInfoDecoder**             | decoder instance of jdbc link info                                     | blank                     |
| **jdbcLinkInfoDecoderClass**        | decoder class of jdbc link info                                        | blank                     |
| **jdbcLinkInfoDecoderClassName**    | decoder class name of jdbc link info                                   | blank                     |

***Object type properties**，choosed priority order：instance > class > class name

***Object type properties**，property class must be not abstract and a constructor without parameters exist in class

***Five defaultxxx properties**(defaultAutoCommit,defaultTransactionIsolationCode,defaultCatalog,defaultSchema,defaultReadOnly), if them not be set,then read value as default from first success creation connection

--- 
**Properties file of configuration**

BeeCP supports loading configuration from properties type files and properties objects(java.util.Properties),a referrence example is blow

```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.loadFromPropertiesFile("d:\beecp\config.properties");
```

config.properties

```properties

username=root
password=root
jdbcUrl=jdbc:mysql://localhost/test
driverClassName=com.mysql.cj.jdbc.Driver

initial-size=1
max-active=10

#implemention class name of connection factory 
connectionFactoryClassName=org.stone.beecp.objects.MockCommonConnectionFactory
#implemention class name of link info decoder
jdbcLinkInfoDecoderClassName=org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder

```

--- 
**Driver parameters**

BeeCP internally uses drivers or connection factories to create connection objects, and factories may depend on some parameters. Two methods are provided in the configuration object (BeeDataSourceConfig) to for it

* ``` addConnectProperty(String,Object);// Add a parameter ```

* ``` addConnectProperty(String);// Add multiple parameters, character format reference: cachePrepStmts=true&prepStmtCacheSize=250  ```


Example for this

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

---
**Blocking and interruption**

Due to network, server, or other reasons, the client is unable to establish a connection with the database, resulting in the connection creator thread of the client being blocked, which affects the use of the connection pool. BeeCP provides two methods on the data source object (BeeDataSource)

* ``` getPoolMonitorVo();//Query method, the result object constains that number of idle connections, borrowed connections, creating connections, creating timeouts, etc ```

* ``` interruptConnectionCreating(boolean);//If thread is blocked during creation a connection,calling this method can be used to end the blocking;If method parameter is true,only interrupt timeout creation ```


<sup>**additional description**</sup></br>
1：The creation timeout is same to maxWait.For example:if this value is 8 seconds,and no connection returned from driver or connection factory,this called as creation timeout</br>
2：After creation timeout and not be interrupted, BeeCP timed thread will scan out them and interrupted them</br>
3：Connecton Creation info of pool also display on BeeCP monitor page and provide interruption button on page


--- 
**Clean and Reinitialization**

BeeCP provides two clear methods on the data source (BeeDataSource) to clean up the connections created in the pool and restore the pool to its initial state,not accept external requests during clean

* ```clear(boolean forceCloseUsing);//forceCloseUsing is true,then recyle borrowed conenction by force ```

* ```clear(boolean forceCloseUsing, BeeDataSourceConfig config);//forceCloseUsing is true,then recyle borrowed conenction by force；then reinitiaize pool with new configuration```

*_Interrupt them if connection creation exist druing clean process;let waiters to exit waiting for ending request of connection getting_

--- 
**Connection Eviction**

During the use of connections, SQL exceptions may occur, some of which are normal exceptions, while others are more serious issues that need to be removed (evicted) from the pool; How to identify exceptions that need to be expelled, BeeCP provides three configuration methods

* A. configuration of exception code：``` addSqlExceptionCode(int code)；//related to SQLException.vendorCode ```

* B. configuration of exception state：``` addSqlExceptionState(String state)；/related to SQLException.SQLState```

* C. configuration of predicate：``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n);```
    
<sup>**additional description**</sup></br>
1：If predicate set, then ignore the other two configurations;evict connection from pool where check reuslt of sql exception is not null/empty</br>
2：If predicate not configured,exception code check is priority to exception state check, if matched,then evict connections</br>
3：Force eviction,call abort method of connection(connect.abort (null))</br>
4：After eviction,if exist waiter for connection transfer,then create a new conenction and transfer it to waiter  

--- 
**Factory customization**

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



