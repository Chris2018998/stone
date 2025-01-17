[🏠](../../README.md) [English](beeop_readme_eng.md)|[中文](beeop_readme_cn.md)

<a><img src="https://img.shields.io/badge/Java-8+-green.svg"></a>
<a><img src="https://maven-badges.herokuapp.com/maven-central/io.github.chris2018998/stone/badge.svg"></a>
[![License](https://img.shields.io/github/license/Chris2018998/stone?color=4D7A97&logo=apache)](https://github.com/Chris2018998/stone/blob/main/LICENSE)

## :coffee: 简介 

BeeOP：一款小型Java对象池组件 

## 性能测试
100万次借用/归还(1000线程 x1000次),获取时间分布,平均时间
|  对象池名    | commons-pool2-2.9.0 | BeeOP0.3_Fair      | BeeOP0.3_Compete  |
| ----------- |----------------     | -------------------| -------------      |  
| 平均时间     | 2.677456            | 0.000347           |  0.000187          |

测试配置：PC:I5-4210M(2.6赫兹，双核4线程),12G内存 Java:JAVA8_64 Pool:初始10,最大10

日志文件：[https://github.com/Chris2018998/BeeOP/blob/main/doc/temp/ObjectPool.log](https://github.com/Chris2018998/BeeOP/blob/main/doc/temp/ObjectPool.log)

源码位置：[https://github.com/Chris2018998/BeeOP/blob/main/doc/temp/BeeOP_Test.rar](https://github.com/Chris2018998/BeeOP/blob/main/doc/temp/BeeOP_Test.rar)

## 范例
```java
public interface Book {
    public String getName();
    public long getNumber();
}
public class JavaBook implements Book{
    private String name;
    private long number;
    public JavaBook() {
        this("Java核心技术·卷2", System.currentTimeMillis());
    }
    public JavaBook(String name, long number) {
        this.name = name;
        this.number = number;
    }
    public String getName() {
        return name;
    }
    public long getNumber() {
        return number;
    }
    public String toString() {
        return name;
    }
}
```
 
```java
public class JavaBookFactory implements RawObjectFactory {
    public Object create() throws ObjectException {
        return new JavaBook("Java核心技术·卷1", System.currentTimeMillis());
    }
    public void setDefault(Object obj) throws ObjectException {
    }
    public void reset(Object obj) throws ObjectException {
    }
    public void destroy(Object obj) {
    }
    public boolean isValid(Object obj, int timeout) {
        return true;
    }
}
```
 
```java
 public class TestBookPool{
   public static void main(String[]){
       BeeObjectSourceConfig config = new BeeObjectSourceConfig();
       config.setObjectFactoryClass(JavaBookFactory.class);
       BeeObjectSource obs = new BeeObjectSource(config);
       
       BeeObjectHandle handle = null;
       try {
            handle = obs.getObject();
            Object v=handle.call("getName");
            System.out.println("Book name:"+v);
        } catch (BeeObjectException e) {
        } finally {
            if (handle != null)
                handle.close();
        }
     }
 }
```

## 功能支持

1：对象借用超时

2：对象借用支持公平与竞争模式

3：支持对象安全关闭

4：断网对象池自动恢复

5：闲置超时和持有超时处理

6：若对象发生异常，池自动增补

7：对象回收时重置

8：对象池重置

9：支持JMX

## 配置项说明

|             配置项      |   描述                          |   备注                                                      |
| ---------------------  | ----------------------------    | -----------------------------------------------------------|
| poolName	             |池名                              |如果未赋值则会自动产生一个                                      |
| fairMode               |是否公平模式                       |默认false,竞争模式                                            |
| initialSize            |池初始创建对象数                   | 默认为0                                                      |
| maxActive              |池最大创建对象数                   | 默认为10个                                                   | 
| borrowSemaphoreSize    |对象借线程最大并行数                |默认取最大对象数/2与cpu核心数的最小值                            |
| maxWait                |对象借用等待最大时间(毫秒)           |默认8秒，对象请求最大等待时间                                   |
| idleTimeout            |对象闲置最大时间(毫秒)              |默认3分钟，超时会被清理                                         |  
| holdTimeout            |对象被持有不用最大时间(毫秒)         |默认5分钟，超时会被清理                                         |  
| forceCloseObject       |是否需要暴力关闭对象                |池关闭或重置，使用，默认false;true:直接关闭使用中对象，false:等待处于使用中归还后再关闭|
| waitTimeToClearPool    |延迟清理的时候时间（秒）             |默认3秒，非暴力清理池下，还存在使用中的对象，延迟等待时间再清理     |      
| idleCheckTimeInterval  |对象闲置扫描线程间隔时间(毫秒)       |默认5分钟                                                   |
| objectFactoryClassName |自定义的对象工厂类名                |默认为空                                                    |
| enableJmx              |JMX监控支持开关                    |默认false                                                  |
