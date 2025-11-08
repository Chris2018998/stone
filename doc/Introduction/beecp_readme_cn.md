
[🏠](../../README.md) [English](beecp_readme_eng.md)|[中文](beecp_readme_cn.md)

BeeCP是一款轻量级JDBC连接池，具有代码少，依赖少，性能高，覆盖率高等特点；技术优点：单连接缓存，固定长度数组，非移动等待，异步加法等.

##
✨**亮点功能**

* 支持阻塞中断操作
* 支持重启和配置重载
* 提供接口支持扩展
* 支持虚拟线程应用
* [提供内外置监控功能](https://github.com/Chris2018998/beecp-starter)

![image](https://github.com/user-attachments/assets/e0684ff2-8a7e-4a20-ab68-69c7b2f30bfa)<br/>

![image](https://github.com/user-attachments/assets/b59dbac9-a3b3-4173-9ff5-845783691e0d)

_温馨提示：如果您的项目是基于springboot框架构建，且有意向使用BeeCP连接池，那么推荐[beecp-starter](https://github.com/Chris2018998/beecp-starter)

*********************************************************************

📊***性能对比***

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>


🍒***差异对比***

| 对比项               | HikariCP               | BeeCP                   |
|---------------------|-------------------------|-------------------------|
| 连接缓存             | 多个                    | 单个                    |
| 连接存储             | CopyOnWriteArrayList   | 固定长度数组              |
| 等待队列             | SynchronousQueue       | ConcurrentLinkedQueue   |
| 连接补充             | 线程池                  | 单线程                   |
| 并行创建             | 不支持                  | 支持                    |
| 重启与重载           | 不支持                  | 支持                    |
| 提供中断             | 未提供                  | 提供                    |
| 扩展接口             | 1                      | 6                       |
| 可禁用ThreadLocal   | 不可                    | 可                       |
| 支持XAConnection    | 不支持                  | 支持                     |

_[**HikariCP**](https://github.com/brettwooldridge/HikariCP)是一款非常优秀的开源作品，它由美国资深专家brettwooldridge开发_


*********************************************************************

⏰***敏捷性测试***

正如著名的[池5秒超时测试](https://github.com/brettwooldridge/HikariCP/wiki/Bad-Behavior:-Handling-Database-Down)所示，HikariCP作者Brettwooldridge曾通过四个连接池验证数据库宕机场景下的超时响应能力，结果仅有HikariCP能在5秒内作出反应。我们针对BeeCP进行了相同测试。[查看测试源码](../beecp/test/src/main/java/org/stone/beecp/other/DbDownTest.java)

|     Requirement          | Settig                                                         |  Remark                                                                                             |
|--------------------------|----------------------------------------------------------------|----------------------------------------------------------------------------------------------------- |
| database                 | mysql-8.4.3                                                    |                                                                                                      |
| driver                   | mysql-connector-j-8.3.0.jar                                    |                                                                                                      |
| url                      | jdbc:mysql://hostIP/test?connectTimeout=50&socketTimeout=100   |the connectTimeout is socket level parameter of mysql jdbc driver                                     |
| timeout                  | **5000** milliseconds                                          |HikariConfig.setConnectionTimeout(5000); BeeDataSourceConfig.setMaxWait(5000);                        |
| Pool version             | HikariCP-6.2.1, stone-1.4.6                                    |                                                                                                      |
| Java version             | Java-22.0.2                                                    |                                                                                                      |

![image](https://github.com/user-attachments/assets/4cca47e0-04d2-4792-a070-1bf9f1bd0306)

**使用18000毫秒重测**
|     Requirement          | Settig                                                         |  Remark                                                                                             |
|--------------------------|----------------------------------------------------------------|---------------------------------------------------------------------------------------------------- |
| timeout                  | **18000** milliseconds                                         |HikariConfig.setConnectionTimeout(18000); BeeDataSourceConfig.setMaxWait(18000);                     |
| Others                   | No Change                                                      |                                                                                                     |
 
![image](https://github.com/user-attachments/assets/4e0d70b4-e68a-4b28-b1c8-bfb0a949e401)


*^-^ 如果设置一个更大时间，会怎么样?*

**Pool Grading**

| Pool	        |Grade   | Reason                                      |
|--------------|--------|---------------------------------------------|
| HikariCP     | A      |由超时参数决定                                |
| BeeCP        | A+     |Socket级反应                                 |



✈️**PreparedStatement关闭性测试**

我相信很多人都知道Connection、PreparedStatement和ResultSet之间存在依赖关系。如果关闭所有者对象，其打开的对象将自动关闭，但是，有一个例外，让我们做一个测试来验证它。[查看测试源代码](../beecp/test/src/main/java/org/stone/beecp/other/MysqlClosedPreparedStatementTest.java).

![image](https://github.com/user-attachments/assets/f75d5684-ff4f-4ad9-b88e-f453e833ea69)

*^-^ 这是一个问题吗，如何解决?*

*********************************************************************

🔡**配置列表**

| 属性                              | 描述                                                                 | 默认值                    |
|----------------------------------|----------------------------------------------------------------------|--------------------------|
| username                         | 连接数据库的用户名                                                     |空                         |
| password                         | 连接数据库的密码                                                       |空                         |
| jdbcUrl                          | 连接数据库的url                                                        |空                        |
| driverClassName                  | 连接数据库的Jdbc驱动类名                                                |空                        |
| poolName	                       | 连接池名，若未设置，则自动产生                                           |空                        |
| fairMode                         | 连接池是否使用公平模式                                                  |false（非公平模式）         | 
| initialSize                      | 池初始化的连接数                                                       |0                         |
| maxActive                        | 池内最大允许连接数                                                     |10                        | 
| semaphoreSize                    | 池内信号量最大许可数                                                   |min(最大连接数/2,CPU核心数） |
| defaultAutoCommit                | autoCommit默认值                                                     |空                          |
| defaultTransactionIsolation      | transactionIsolation默认值                                           |空                          |
| defaultCatalog                   | catalog默认值                                                        |空                          |
| defaultSchema                    | schema默认值                                                        |空                          |
| defaultReadOnly                  | readOnly默认值                                                      |空                          |
| maxWait                          | 借用连接时的最大等待时间(毫秒)                                         |8000                |
| idleTimeout                      | 未借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                    |18000               |  
| holdTimeout                      | 已借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                    |0                   |  
| aliveTestSql                     | 连接活性检查sql                                                      |SELECT 1            |  
| aliveTestTimeout                 | 连接存活检测结果的等待最大时间(秒)                                      |3                   |  
| aliveAssumeTime                  | 存活检测阈值时间差，小于则假定为活动连接，大于则检测                       |500                 |  
| forceRecycleBorrowedOnClose      | 清理时，是否强制回收已借连接                                            |false               |
| parkTimeForRetry                 | 清理时，等待已借连接返回池中的时间(毫秒)                                 |3000                |             
| intervalOfClearTimeout           | 池内定时线程工作隔时间(毫秒)                                            |18000               |
| forceDirtyWhenSetSchema          | schema属性是否强制重置标记(PG可设置）                                   |false               |
| forceDirtyWhenSetCatalog         | catalog属性是否强制重置标记(PG可设置）                                  |false               |
| useThreadLocal                   | ThreadLocal是否启用（false时可支持虚拟线程）                             |true                | 
| registerMbeans                   | JMX监控支持开关                                                           |false            | 
| printConfiguration               | 是否打印配置信息                                                           |false               | 
| printRuntimeLogs                 | 是否打印运行时日志                                                         |false               | 
| **connectionFactory**            | 连接工厂实例                                                              |空                   |
| **connectionFactoryClass**       | 连接工厂类                                                               |空                   |
| **connectionFactoryClassName**   | 连接工厂类名                                                              |空                   |
| **predicate**                    | 异常断言实例                                                              |空                   |
| **predicateClass**               | 异常断言类                                                                |空                   |
| **predicateClassName**           | 异常断言类名                                                              |空                   |
| **linkInfoDecoder**              | 连接信息解码器                                                             |空                   |
| **linkInfoDecoderClass**         | 连接信息解码器类                                                            |空                   |
| **linkInfoDecoderClassName**     | 连接信息解码器类名                                                           |空                   |
| enableMethodExecutionLogCache    | 方法执行日志缓存开关,默认不打开                                               |false                 |
| methodExecutionLogCacheSize      | 方法执行日志缓存大小                                                         |1000                   |
| methodExecutionLogTimeout        | 方法执行日志缓存超时时间(毫秒)                                                |180000             |
| intervalOfClearTimeoutExecutionLogs | 方法执行日志缓存清理间隔时间(毫秒)                                         |180000               |
| slowConnectionThreshold             | 慢连接的阈值(毫秒)                                                        |30000                 |
| slowSQLThreshold                    | 慢SQL的阈值(毫秒)                                                        |30000                 |
| **methodExecutionListener**         | 方法执行监听器                                                            | 空                      |
| **methodExecutionListenerClass**    | 方法执行监听器类                                                           | 空                      |
| **methodExecutionListenerClassName** | 方法执行监听器类名                                                         | 空                      |
| **methodExecutionListenerFactory**          |方法执行监听器工厂                                                   | 空                      |
| **methodExecutionListenerFactoryClass**     |方法执行监听器工厂类                                                  | 空                      |
| **methodExecutionListenerFactoryClassName** | 方法执行监听器工厂类名                                               | 空                      |

*_**对象级属性**，设置的是类或类名时，须存在无参构造器，生效选择次序：实例 > 类 > 类名_  

*********************************************************************


##
📝**文件配置**

BeeCP支持从属性文件（*.properities）或属性对象（java.util.properities）中读取参数信息到配置对象上，参考例子如下

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

#连接工厂实现的类名
connectionFactoryClassName=org.stone.beecp.objects.MockCommonConnectionFactory
#jdbc link信息的解码器实现的类名
jdbcLinkInfoDecoderClassName=org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder

```
_温馨提示：属性名配置方式目前支持：驼峰，中划线，下划线_

##
⚙**驱动参数**

BeeCP内部是使用驱动或连接工厂创建连接对象，它们可能依赖一些参数，在配置对象(BeeDataSourceConfig)提供了两个方法

* ```addConnectProperty(String,Object);//添加单个参数 ```

* ```addConnectProperty(String);//以字符串的方式添加参数，可一次配置多个，如：cachePrepStmts=true&prepStmtCacheSize=250```

<br/>

_参考代码_

```java
 BeeDataSourceConfig config = new BeeDataSourceConfig();
 config.addConnectProperty("cachePrepStmts", "true");
 config.addConnectProperty("prepStmtCacheSize", "250");
 config.addConnectProperty("prepStmtCacheSqlLimit", "2048");

 //或者
 config.addConnectProperty("cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048");

 //或者
 config.addConnectProperty("cachePrepStmts:true&prepStmtCacheSize:250&prepStmtCacheSqlLimit:2048");
```

* _文件配置1_
```properties

connectProperties=cachePrepStmts=true&prepStmtCacheSize=50

```

* _文件配置2(多项参数时推荐)_
```properties
connectProperties.size=2
connectProperties.1=prepStmtCacheSize=50
connectProperties.2=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true
```

## 
🔚**连接驱逐**

BeeCP提供了两种方式

1. 手工驱逐，调用连接上的abort方法（connecton.abort(null)），连接池立即对它们进行物理关闭，并从池中移除

2. 配置驱逐，用于帮助连接池识别需要驱逐发生SQL异常的连接，三种配置

* A. 异常代码配置：``` addSqlExceptionCode(int code)；//对应SQLException.vendorCode ```

* B. 异常状态配置：``` addSqlExceptionState(String state)；/对应SQLException.SQLState```

* C. 异常断言配置：``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n); ```
 
<br/>

_文件配置_
```properties

sqlExceptionCodeList=500150,2399,1105
sqlExceptionStateList=0A000,57P01,57P02,57P03,01002,JZ0C0,JZ0C1

//或者
evictPredicateClassName=org.stone.beecp.objects.MockEvictConnectionPredicate

```

_补充说明_

* 1：断言驱逐用于自定义性实现，当其验证结果非空（Not Null and Not Empty）则驱逐连接
* 2：断言配置的使用优先于代码配置和状态配置，若存在断言配置，自动忽略其他两项配置
* 3：异常代码检查优先于异常状态检查
* 4：驱逐后，若池种存在等待者，自动候补一个新连接

##
✂**中断处理**

连接创建是连接池内一项目重要活动，但是由于服务器或网络或其他原因，可能导致创建过程处于阻塞状态，为解决这一问题，BeeCP提供了两种方式

1. 外部方式，在数据源对象（BeeDataSource）提供两个方法：查询方法：getPoolMonitorVo()；中断方法：interruptConnectionCreating(boolean)；

2. 内部方式，内部工作线程定时识别阻塞，并中断它们<br/>

<br/>

_补充说明_

* 1：创建时间超过maxwait的值时，连接池则判断定为创建阻塞
* 2：中断的是借用者线程，getConnection上会抛出中断异常；若是候补线程，它会尝试将异常传递给等待者
* 3: BeeCP监控页面上也可查看到相关信息，如创建数，创建超时数，如超时则显示出中断按钮

##
🛒**清理与重启**

BeeCP支持重置操作，让连接池恢复到初始状态，清理过程中不接受外部请求，它主要完成两个事项

* A: 清除池内所有的连接和等待者
* B: 重新初始化连接池（也可是使用新配置）

<br/>

_主要有两个方法_

* ```BeeDataSource.clear(boolean forceCloseUsing);//使用原配置重新初始化 ```

* ```BeeDataSource.clear(boolean forceCloseUsing, BeeDataSourceConfig newConfig);//使用新配置重新初始化```


##
🏭**连接工厂接口**

在BeeCP内部定义了连接工厂接口，并内置两种基本实现（对驱动和数据源的封装），工厂接口是允许外部自定义实现，有4个相关配置方法（etConnectionFactory，setXaConnectionFactory，setConnectionFactoryClass，setConnectionFactoryClassName）分别设置工厂实例，工厂类，工厂类名，下面是一个参考例子

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
        this.driver= driver;
        this.connectInfo = connectInfo;
    }

    public Connection create() throws SQLException {
        return driver.connect(url, connectInfo);
    }
}


public class MyConnectionDemo {
    public static void main(String[] args) throws SQLException {
        final String url = "jdbc:mysql://localhost:3306/test";
        final Driver driver = DriverManager.getDriver(url);
        final Properties connectInfo = new Properties();
        connectInfo.put("user","root");
        connectInfo.put("password","root");

        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactory(new MyConnectionFactory(url, connectInfo, driver));
        BeeDataSource ds = new BeeDataSource(config);

        try (Connection con = ds.getConnection()) {
            //put your code here
        }
    }
}

```

_温馨提示：若同时设置连接工厂和驱动类参数（driver,url,user,password)，那么连接工厂被优先使用。_
