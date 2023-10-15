<a><img src="https://img.shields.io/badge/JDK-1.8+-green.svg"></a>
<a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
<a><img src="https://maven-badges.herokuapp.com/maven-central/io.github.chris2018998/stone/badge.svg"></a>
 
## :coffee: 简 介

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

## 🐝 beecp

一款简单易用的JDBC连接池，具有性能高，代码轻，稳定好的特点；它支持多种参数灵活设置，适置多种主流据库驱动；健壮性好以及良好接口扩展性; 产品亮点：无锁应用，单点缓存，非移动等待，Transfer队列复用

![图片](https://user-images.githubusercontent.com/32663325/153597592-c7d36f14-445a-454b-9db4-2289e1f92ed6.png)

### 🍒 对比光连接池（HikariCP）

| **比较项**                      |**BeeCP**                                          | **HikariCP**                                      |
|---------------------------------|---------------------------------------------------| ------------------------------------------------- |
| 关键技术                         |ThreadLocal，信号量，ConcurrentLinkedQueue，Thread   | FastList，ConcurrentBag，ThreadPoolExecutor       |
| 相似点                           |CAS使用，代理预生成，使用驱动自带Statement缓存          |                                                  |
| 差异点                           |支持公平模式，支持XA分布事务，强制回收持有不用的连接，单点缓存，队列复用，非移动等待，独创自旋控制/连接传递程序片段|支持池暂停|
| 文件                             |37个源码文件，Jar包95KB                              |44个源码文件，Jar包158KB                                   |
| 性能                             |总体性能高40%以上（光连接池基准）                      |                                                         |
