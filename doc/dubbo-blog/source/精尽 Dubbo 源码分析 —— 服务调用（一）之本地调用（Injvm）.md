# 精尽 Dubbo 源码分析 —— 服务调用（一）之本地调用（Injvm）



# 1. 概述

从这篇文章开始，我们开始分享**服务调用**的实现。在前面，艿艿已经写了服务：

- 本地暴露、远程暴露
- 本地引用、远程引用

那么在服务调用，必然也是分：

- 本地调用
- 远程调用

本文分享**本地调用**，在 `dubbo-rpc-injvm` 模块实现。

相比远程调用，实现上会简单很多：因为调用的服务，就在本地进程内，且不存在多个，所以不需要**集群容错**和**网络通信**相关的功能。

# 2. 调试环境

> 友情提示：笔者建议胖友先尝试自己搭建本地调用的调试环境，如果碰到问题在看本小节。

基于 `dubbo-demo-consumer` 改造：

1、将 `dubbo-demo-provider` 模块的 `com.alibaba.dubbo.demo.provider.DemoServiceImpl` 类，复制到 `dubbo-demo-consumer` 模块的 `com.alibaba.dubbo.demo.consumer` 包下。

2、在 `resources/META-INF/spring` 目录下，新建 `dubbo-demo-injvm.xml` 文件，内容如下：

```
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://code.alibabatech.com/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd
       http://code.alibabatech.com/schema/dubbo http://code.alibabatech.com/schema/dubbo/dubbo.xsd">

    <dubbo:application name="demo-injvm"/>

    <dubbo:registry address="N/A"/>

    <dubbo:reference id="demoService" interface="com.alibaba.dubbo.demo.DemoService" protocol="injvm" scope="local" />

    <bean id="demoServiceImpl" class="com.alibaba.dubbo.demo.consumer.DemoServiceImpl"/>
    <dubbo:service interface="com.alibaba.dubbo.demo.DemoService" ref="demoServiceImpl" protocol="injvm" scope="local" />

</beans>
```

3、修改 `com.alibaba.dubbo.demo.consumer.Consumer` 类，加载的 Spring 配置文件为 `dubbo-demo-injvm.xml` ，代码如下：

```
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-injvm.xml"});
```

4、启动 Consumer ，即可开始调试。

------

[`dubbo-demo-consumer`](https://github.com/YunaiV/dubbo/tree/f14e4e4fffaede31cbece589e0f543ec6669b2ae/dubbo-demo/dubbo-demo-consumer) ，是笔者改完，可运行的一个快照版本。

# 3. 顺序图

- 消费者调用服务的顺序图：![消费者调用服务的顺序图](http://www.iocoder.cn/images/Dubbo/2018_10_01/01.png)
- 提供者提供服务的顺序图：![提供者提供服务的顺序图](http://www.iocoder.cn/images/Dubbo/2018_10_01/02.png)

🙂 流程上还是比较简单的，笔者就不哔哔了。如果胖友不太理解，可以回看之前的文章，再多多调试理解，或者知识星球发帖一起讨论。

下面，我们来看每个步骤的实现代码。

# 4. 消费者调用服务

## 4.1 Proxy

> 提示：对应图中 [1] [2] [3]

见 [《精尽 Dubbo 源码分析 —— 动态代理（一）之 Javassist》](http://svip.iocoder.cn/Dubbo/proxy-javassist/?self) 文章。

## 4.2 ProtocolFilterWrapper

> 提示：对应图中 [5]

ProtocolFilterWrapper 的带有过滤链的 Invoker ，整个调用过程和 J2EE FilterChain 是一致的，具体每个 Dubbo Filter 的实现，我们另开文章。

```
for (int i = filters.size() - 1; i >= 0; i--) {
    final Filter filter = filters.get(i);
    final Invoker<T> next = last;
    last = new Invoker<T>() {

        public Class<T> getInterface() {
            return invoker.getInterface();
        }

        public URL getUrl() {
            return invoker.getUrl();
        }

        public boolean isAvailable() {
            return invoker.isAvailable();
        }

        public Result invoke(Invocation invocation) throws RpcException {
            return filter.invoke(next, invocation);
        }

        public void destroy() {
            invoker.destroy();
        }

        @Override
        public String toString() {
            return invoker.toString();
        }
    };
}
```

`#invoke(invocation)` 方法中，调用 `Filter#(invoker, invocation)` 方法，不断执行过滤逻辑。而在 Filter 中，又不断调用 `Invoker#invoker(invocation)` 方法，最终最后一个 Filter ，会调用 `InjvmInvoker#invoke(invocation)` 方法，继续执行逻辑。

> 友情提示，InjvmInvoker 只是此处的例子，不同的协议，会调用不同的 Invoker 实现类，例如 Dubbo 协议，调用的是 DubboInvoker 。

另外，Filter 调用 Invoker 的示例如下：

```
public class DemoFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation); // 调用
    }
}
```

## 4.3 ListenerInvokerWrapper

> 提示：对应图中 [6]

ListenerInvokerWrapper 类，主要目的是为了 InvokerListener 的触发，目前该监听器只有 `#referred(invoker)``#destroyed(invoker)` 两个接口方法，并未对 `#invoke(invocation)` 的过程，实现监听。因此，ListenerInvokerWrapper 的 `#invoke(invocation)` 的实现基本等于零，代码如下：

```
@Override
public Result invoke(Invocation invocation) throws RpcException {
    return invoker.invoke(invocation);
}
```

## 4.4 AbstractInvoker

> 提示：对应图中 [7]

AbstractInvoker ，在 `#invoke(invocation)` 方法中，实现了**公用逻辑**，同时**抽象**了 `#doInvoke(invocation)` 方法，子类实现自定义逻辑。代码如下：

```
 1: public Result invoke(Invocation inv) throws RpcException {
 2:     if (destroyed.get()) {
 3:         throw new RpcException("Rpc invoker for service " + this + " on consumer " + NetUtils.getLocalHost()
 4:                 + " use dubbo version " + Version.getVersion()
 5:                 + " is DESTROYED, can not be invoked any more!");
 6:     }
 7:     RpcInvocation invocation = (RpcInvocation) inv;
 8:     // 设置 `invoker` 属性
 9:     invocation.setInvoker(this);
10:     // 添加公用的隐式传参，例如，`path` `interface` 等等，详见 RpcInvocation 类。
11:     if (attachment != null && attachment.size() > 0) {
12:         invocation.addAttachmentsIfAbsent(attachment);
13:     }
14:     // 添加自定义的隐士传参
15:     Map<String, String> context = RpcContext.getContext().getAttachments();
16:     if (context != null) {
17:         invocation.addAttachmentsIfAbsent(context);
18:     }
19:     // 设置 `async=true` ，若为异步方法
20:     if (getUrl().getMethodParameter(invocation.getMethodName(), Constants.ASYNC_KEY, false)) {
21:         invocation.setAttachment(Constants.ASYNC_KEY, Boolean.TRUE.toString());
22:     }
23:     RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);
24:
25:     // 执行调用
26:     try {
27:         return doInvoke(invocation);
28:     // TODO 【8023 biz exception】
29:     } catch (InvocationTargetException e) { // biz exception
30:         Throwable te = e.getTargetException();
31:         if (te == null) {
32:             return new RpcResult(e);
33:         } else {
34:             if (te instanceof RpcException) {
35:                 ((RpcException) te).setCode(RpcException.BIZ_EXCEPTION);
36:             }
37:             return new RpcResult(te);
38:         }
39:     } catch (RpcException e) {
40:         if (e.isBiz()) {
41:             return new RpcResult(e);
42:         } else {
43:             throw e;
44:         }
45:     } catch (Throwable e) {
46:         return new RpcResult(e);
47:     }
48: }
```

- 第 7 至 23 行：设置 `invocation` 的属性。

  - 第 9 行：设置 `invoker` 属性为自己。在上面，我们已经看到 Invoker 是层层嵌套，只要到了这里才是真正的 Invoker 对象。

  - 第 10 至 13 行：添加**公用的**的隐式传参。例如，`path` `interface` 等等。所有见 [RpcInvocation](https://github.com/apache/incubator-dubbo/blob/master/dubbo-rpc/dubbo-rpc-api/src/main/java/com/alibaba/dubbo/rpc/RpcInvocation.java#L50-L76) 构造方法。从 `Invocation#addAttachmentsIfAbsent(context)` 方法，不存在才添加，因此业务上隐式传参的 KEY 不能冲突到这几个。

  - 第 14 至 18 行：添加**自定义的**隐式传参，从 `RpcContext.attachments` 中。使用 RpcContext 隐式传参需要注意：

    > 注意：RpcContext 是一个临时状态记录器，当接收到 RPC 请求，或发起 RPC 请求时，RpcContext 的状态都会变化。
    > 比如：A 调 B，B 再调 C，则 B 机器上，在 B 调 C 之前，RpcContext 记录的是 A 调 B 的信息，在 B 调 C 之后，RpcContext 记录的是 B 调 C 的信息。

  - 第 19 至 23 行：异步方法，相关的处理，后面文章分享。

- 第 27 行：调用 `#doInvoke(invocation)` **抽象**方法，实现不同协议自定义的调用实现。代码如下：

  ```
  protected abstract Result doInvoke(Invocation invocation) throws Throwable;
  ```

- 第 28 至 47 行：// TODO 【8023 biz exception】

看完这个方法，我们可以看到，一次 Dubbo RPC ，涉及到抽象模型如下图：

![RPC](http://www.iocoder.cn/images/Dubbo/2018_10_01/03.png)

## 4.5 InjvmInvoker

> 提示：对应图中 [8]

`#doInvoke(invocation)` 实现方法，代码如下：

```
/**
 * Exporter 集合
 *
 * key: 服务键
 *
 * 该值实际就是 {@link com.alibaba.dubbo.rpc.protocol.AbstractProtocol#exporterMap}
 */
private final Map<String, Exporter<?>> exporterMap;

  1: @Override
  2: public Result doInvoke(Invocation invocation) throws Throwable {
  3:     // 获得 Exporter 对象
  4:     Exporter<?> exporter = InjvmProtocol.getExporter(exporterMap, getUrl());
  5:     if (exporter == null) {
  6:         throw new RpcException("Service [" + key + "] not found.");
  7:     }
  8:     // 设置服务提供者地址为本地
  9:     RpcContext.getContext().setRemoteAddress(NetUtils.LOCALHOST, 0);
 10:     // 调用
 11:     return exporter.getInvoker().invoke(invocation);
 12: }
```

- 第 3 至 7 行：调用

   

  ```
  InjvmProtocol#getExporter(exporterMap, url)
  ```

   

  方法，获得对应的 Exporter 对象。在

   

  《精尽 Dubbo 源码分析 —— 服务暴露（一）之本地暴露（Injvm）》

   

  中，我们已经看到，

  ```
  exporterMap
  ```

   

  属性，就是从 InjvmProtocol 的

   

  ```
  exporterMap
  ```

   

  属性。

  - 在远程调用中，选择服务提供者的逻辑会更加复杂，后续文章见。

- 第 9 行：设置服务提供者地址为本地。

- 第 11 行：获得到 Exporter 对象，里面就有**服务提供者的 Invoker 对象**。调用 `Invoker#invoke(invocation)` 方法，调用服务。

# 5. 提供者提供服务

## 5.1 InjvmInvoker

> 提示：对应图中 [1] [2]

在 [「4.5 InjvmInvoker」](http://svip.iocoder.cn/Dubbo/rpc-injvm/#) 已经分享。

## 5.2 ProtocolFilterWrapper

> 提示：对应图中 [3] [4]

在 [「4.2 ProtocolFilterWrapper」](http://svip.iocoder.cn/Dubbo/rpc-injvm/#) 基本一致，差异点在服务消费者和提供者的过滤器是**不同**的。

## 5.3 DelegateProviderMetaDataInvoker

> 提示：对应图中 [5]

DelegateProviderMetaDataInvoker ，带有服务提供者配置 ServiceConfig 的 Invoker 对象。从目前代码上来看，ServiceConfig 暂时没用到。

`#invoke(invocation)` 方法，代码如下：

```
/**
 * Invoker 对象
 */
protected final Invoker<T> invoker;
/**
 * 服务提供者配置
 */
private ServiceConfig metadata;

@Override
public Result invoke(Invocation invocation) throws RpcException {
    return invoker.invoke(invocation);
}
```

## 5.4 Wrapper

> 提示：对应图中 [6] [7] [8] [9]

见 [《精尽 Dubbo 源码分析 —— 动态代理（一）之 Javassist》](http://svip.iocoder.cn/Dubbo/proxy-javassist/?self) 文章。

# 666. 彩蛋

![知识星球](http://www.iocoder.cn/images/Architecture/2017_12_29/01.png)

通过 InjvmProtocol 的调用过程，我们可以很容易理清 Dubbo 调用的过程。下一文，DubboProtocol 走起！