<a><img src="https://img.shields.io/badge/JDK-1.8+-green.svg"></a>
<a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
<a><img src="https://maven-badges.herokuapp.com/maven-central/io.github.chris2018998/stone/badge.svg"></a>
 
## 🍑 简 介

Stone, 一款小型Java工具包，它整合了4个轻量级J2ee组件池，它们彼此独立，互不依赖。

![图片](https://github.com/Chris2018998/stone/assets/32663325/25f3cf51-c479-4218-9e02-bbe96ea1ab4f)

## :arrow_down: Maven坐标 

```xml
<dependency>
   <groupId>io.github.chris2018998</groupId>
   <artifactId>stone</artifactId>
   <version>1.2.3</version>
</dependency>
```

## 池一：🐝beecp🌺

一款简单易用的JDBC连接池，具有性能高，代码轻，稳定好的特点；它支持多种参数灵活设置，适置多种主流据库驱动；健壮性好以及良好接口扩展性; 产品亮点：无锁应用，单点缓存，非移动等待，Transfer队列复用

![图片](https://user-images.githubusercontent.com/32663325/153597592-c7d36f14-445a-454b-9db4-2289e1f92ed6.png)

#### 🍒 对比光连接池（HikariCP）

| **比较项**                      |**BeeCP**                                          | **HikariCP**                                      |
|---------------------------------|---------------------------------------------------| ------------------------------------------------- |
| 关键技术                         |ThreadLocal，信号量，ConcurrentLinkedQueue，Thread   | FastList，ConcurrentBag，ThreadPoolExecutor       |
| 相似点                           |CAS使用，代理预生成，使用驱动自带Statement缓存          |                                                  |
| 差异点                           |支持公平模式，支持XA分布事务，强制回收持有不用的连接，单点缓存，队列复用，非移动等待，独创自旋控制/连接传递程序片段|支持池暂停|
| 文件                             |37个源码文件，Jar包95KB                              |44个源码文件，Jar包158KB                                   |
| 性能                             |总体性能高40%以上（光连接池基准）                      |                                                         |

#### 运行时监控

提供三种监控方式：Jmx监控，状态VO，控制台方式

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)

![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

#### 使用例子
###### :point_right: 例子1(独立应用)
```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.setDriverClassName("com.mysql.jdbc.Driver");
config.setJdbcUrl("jdbc:mysql://localhost/test");
config.setUsername("root");
config.setPassword("root");
BeeDataSource ds=new BeeDataSource(config);
Connection con=ds.getConnection();
....

```
###### :point_right: 例子2(Springbooot)
*application.properties*
```java
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.url=jdbc:mysql://localhost/test
spring.datasource.driverClassName=com.mysql.jdbc.Driver
``` 

*DataSourceConfig.java*
```java
@Configuration
public class DataSourceConfig {
  @Value("${spring.datasource.username}")
  private String user;
  @Value("${spring.datasource.password}")
  private String password;
  @Value("${spring.datasource.url}")
  private String url;
  @Value("${spring.datasource.driverClassName}")
  private String driver;

  @Bean
  @Primary
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource primaryDataSource() {
    return DataSourceBuilder.create().type(cn.beecp.BeeDataSource.class).build();
  }
  
  @Bean
  public DataSource secondDataSource() {
   return new BeeDataSource(new BeeDataSourceConfig(driver,url,user,password));
  }
}
```

#### 配置项列表
|项名                              |描述                                   |默认值                               |
| ---------------------------------| -------------------------------------| -----------------------------------|
|username                          |JDBC用户名                             |空                                  |
|password                          |JDBC密码                               |空                                  |
|jdbcUrl                           |JDBC连接URL                            |空                                  |
|driverClassName                   |JDBC驱动类名                            |空                                  |
|poolName	                   |池名，如果未赋值则会自动产生一个                 |空                                  |
|fairMode                          |是否使用公平模式                         |false（竞争模式）                     | 
|initialSize                       |连接池初始大小                           |0                                   |
|maxActive                         |连接池最大个数                           |10                                  | 
|borrowSemaphoreSize               |信号量许可大小                           |min(最大连接数/2,CPU核心数）           |
|defaultAutoCommit                 |AutoComit默认值,未配置则从第一个连接上读取默认值|空                               |
|defaultTransactionIsolationCode   |事物隔离代码，未设置时则从第一个连接上读取默认值|空                                |
|defaultCatalog                    |Catalog默认值 ,未配置则从第一个连接上读取默认值|空                                |
|defaultSchema                     |Schema默认值,未配置则从第一个连接上读取默认值|空                                  |
|defaultReadOnly                   |ReadOnly默认值 ,未配置则从第一个连接上读取默认值|空                               |
|maxWait                           |连接借用等待最大时间(毫秒)                |8000                                |
|idleTimeout                       |连接闲置最大时间(毫秒)                    |18000                               |  
|holdTimeout                       |连接被持有不用最大允许时间(毫秒)           |18000                               |  
|validTestSql                      |连接有效性测试SQL语句                     |SELECT 1                            |  
|validTestTimeout                  |连接有效性测试超时时间(秒)                 |3                                   |  
|validAssumeTime                   |连接测试的间隔时间(毫秒)                   |500                                 |  
|forceCloseUsingOnClear            |是否直接关闭使用中连接                     |false                               |
|delayTimeForNextClear             |延迟清理的时候时间（毫秒）                 |3000                                |  


## 池二：🐝beeop🌷


## 池三：🐝beetp🪷




## 池四：☀️shine🌻
