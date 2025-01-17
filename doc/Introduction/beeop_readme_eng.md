[🏠](../../README.md) [English](beeop_readme_eng.md)|[中文](beeop_readme_cn.md)

<a><img src="https://img.shields.io/badge/Java-8+-green.svg"></a>
<a><img src="https://maven-badges.herokuapp.com/maven-central/io.github.chris2018998/stone/badge.svg"></a>
[![License](https://img.shields.io/github/license/Chris2018998/stone?color=4D7A97&logo=apache)](https://github.com/Chris2018998/stone/blob/main/LICENSE)

## :coffee: Introduction 

BeeOP: a small Java object pool

## Performance 

One million borrow/return (1000 threads x 1000 times)
|    Pool type     |commons-pool2-2.9.0  |  BeeOP-0.3_Fair    | BeeOP-0.3_Compete |
| -----------------|---------------------| -------------------| ----------------- |  
| Average time(ms) | 2.677456            | 0.000347           |  0.000187         |

PC:I5-4210M(2.6Hz，dual core4threads),12G memory Java:JAVA8_64 Pool:init-size10,max-size:10

Test log file：[https://github.com/Chris2018998/BeeOP/blob/main/doc/temp/ObjectPool.log](https://github.com/Chris2018998/BeeOP/blob/main/doc/temp/ObjectPool.log)

Test source：[https://github.com/Chris2018998/BeeOP/blob/main/doc/temp/BeeOP_Test.rar](https://github.com/Chris2018998/BeeOP/blob/main/doc/temp/BeeOP_Test.rar)

## Example

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

## Features

1：Borrow timeout

2：Fair mode and compete mode for borrowing 

3：Proxy object safe close when return

4：Pooled object cleared when network bad,pooled object recreate when network restore OK

5：Idle timeout and hold timeout(long time inactively hold by borrower)

6：Pooled object closed when exception,then create new one and transfer it to waiter

7：Pooled object reset when return

8：Pool can be reset

9：Jmx support

## configuration

|     Field name         |       Description                               |   Remark                                                    |
| ---------------------  | ------------------------------------------------| -----------------------------------------------------------|
|poolName               |pool name                                         | name auto generated when not set                          |
|fairMode               |boolean indicator for borrow fair mode           |true:fair mode,false:comepete mode;default is false         |
|initialSize            |pooled object creation size when pool initialized|default is 0                                                |
|maxActive              |max size for pooled object instances in pool     |default is 10                                               | 
|borrowSemaphoreSize    |borrow concurrent thread size                    |default val=min(maxActive/2,cpu size)                       |                     
|maxWait                |max wait time to borrow one object instance      |time unit is ms,default is 8000 ms                          |                     
|idleTimeout            |max idle time of object instance in pool         |time unit is ms,default is 18000 ms                         |  
|holdTimeout            |max inactive time hold by borrower               |time unit is ms,default is 300000 ms                        |  
|forceCloseUsingOnClear |object close indicator when pool closing or reseting|true:close;false:wait object return, default is false    |          
|delayTimeForNextClear   |park time to clear when checked object is in using state|effected  when forceCloseObject==true               |                     
|idleCheckTimeInterval   |scan thread time interval to check idle object |time unit is ms,default is 300000 ms                         |
|objectFactoryClassName  |object factory class name                      |default is null                                              |
|enableJmx               |JMX boolean indicator for pool                 |default is false                                             |
