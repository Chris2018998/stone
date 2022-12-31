 <a href="https://github.com/Chris2018998/stone/tree/main/doc/Introduction/shine_readme_eng.md">English</a>|<a href="https://github.com/Chris2018998/stone/tree/main/doc/Introduction/shine_readme_cn.md">中文</a>
<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
</p> 

## :coffee: 简介 

Shine 是一款由 Java 语言开发的并发工具包，它采用统一化思想构建，具有代码少，重用高的特点，便于学习和维护；目前主要由两部分组成：同步器 和以同步器为基础框架构建的并发包（类似 JDK 的并发部分）；同步器则是由一些线程等待池组成。

## 设计思想

Shine 的同步方式是通过一些线程等待池来实现的，我们将这些池统统称为：等待 - 唤醒池（计算机领域中首次提出？），整体架构图可表示如下

![图片](https://user-images.githubusercontent.com/32663325/210122916-87e2fe68-0e97-4ffc-809d-677f97bc2c7d.png)


## 等待池介绍

<table>
 <tr>
  <td width=200px>池名</td>
  <td width=200px>池类名</td>
  <td width=800px>池的描述</td>
 </tr>
 <tr>
  <td>结果等待池</td>
  <td>ResultWaitPool.java</td>
  <td>
     1：在池内调用一个 ResultCall 实例，将调用结果使用验证器进行识别，若结果符合预期则退出池，若不符合预期则在池中等待，直到被唤醒再次调用再比较</br>
     2：若等待超时，则会退出等待，并返回一个超时结果（默认是 false)</br>
     3：若等待时被中断，则可抛出 InterruptException</br>
     4：ResultCall 和结果验证器均可自行定义</br>
     5：应用它的相关类：CountDownLatch，CyclicBarrier</br>
   </td>
 </tr>
 
  <tr>
  <td>信号量等待池</td>
  <td>SignalWaitPool.java</td>
  <td>
      1：调用者调用池的等待方法，在池内部等待预期状态值（使用结果验证器，同结果等待池）</br>
      2：若等待超时，则会退出等待，并返回一个超时结果（默认是 false)</br>
      3：若等待时被中断，则可抛出 InterruptException</br>
      4：应用它的相关类：Lock 的 Conndtion 实现类</br>
   </td>
 </tr>
 
 <tr>
  <td>Transfer等待池</td>
  <td>TransferWaitPool.java</td>
  <td>
      应用它的相关类：SynchronousQueue，LinkedTransferQueue
   </td>
 </tr
 <tr>
  <td>状态等待池</td>
  <td>StateWaitPool.java</td>
  <td> </td>
 </tr>

 <tr>
  <td>资源等待池</td>
  <td>ResourceWaitPool.java</td>
  <td>由结果等待池衍生而来，应用它的相关类：Lock, Semaphore</td>
 </tr>
 </table>

## 扩展接口介绍

A:  结果调用接口
```java
public interface ResultCall {

    //do some thing(don't use thread block method in implementation)
    Object call(Object arg) throws Exception;
}
```

B:  结果验证接口

```java
public interface ResultValidator {

    //return this value on wait timeout in pool
    Object resultOnTimeout();

    //check call result or state is whether expected
    boolean isExpected(Object result);
}
```

## 开发进展介绍

*  同步器框架基本完成

*  已开发重入锁和读写锁

*  已开并发包中的 5 个功能点类（排除两个并发队列）

*  线程池正在开发中

*  已开发近 100 个测试案例

 
