# 精尽 Dubbo 源码分析 —— 服务调用（二）之远程调用（Dubbo）【2】同步调用



# 1. 概述

本文分享 `dubbo://` 协议的远程调用的**第二部分：同步调用**。

在 `dubbo://` 协议的调用，一共分成三种：

1. sync 同步调用
2. async 异步调用
3. oneway 单向调用

前两种比较好理解，都是基于 Request Response 模型，差异点在异步调用，服务消费者**不阻塞**等待结果，而是通过**回调**的方式，处理服务提供者返回的结果。
最后一种，基于 Message 模型，发起调用，而不关注等待和关注执行结果。
因此，从性能上：oneway > async > sync 。

> 友情提示：本文会分享 sync 和 oneway 两种方式。

# 2. 顺序图

- 消费者调用服务的顺序图：![顺序图](http://www.iocoder.cn/images/Dubbo/2018_10_04/02_01.jpeg)

  - 此图是在

     

    ```
    injvm://
    ```

     

    协议的顺序图的基础上修改：

    - 将 InjvmInvoker 替换成 DubboInvoker 。
    - 在 `#doInvoker()` 方法中，DubboInvoker 会调用 Client ，向服务提供者发起请求。

  - 可能会有胖友问，

    集群容错

    呢？在 InvokerInvocationHandler 之后，ProtocolFilterWrapper$Invoker 之前。如下图所示：

    

    - 🙂 我们后面专门写几篇文章，专门分享集群容错，所以本文略过。

- 提供者提供服务的顺序图：![顺序图](http://www.iocoder.cn/images/Dubbo/2018_10_04/02_03.jpeg)

  - 此图是在

     

    ```
    injvm://
    ```

     

    协议的顺序图的基础上修改：

    - InjvmInvoker 替换成 ExchangeServer 。例如在 Netty4 中，IO Worker 解析请求，转发给 ExchangeHandler 处理。
    - InjvmProtocol 替换成 DubboProtocol 。在该类中，实现了自定义的 ExchangeHandler 处理请求。**注意**，在 **Dubbo ThreadPool**中处理请求，参见 [《Dubbo 用户指南 —— 线程模型》](http://dubbo.apache.org/zh-cn/docs/user/demos/thread-model.html) 文档。

# 3. 消费者调用服务

调用 `DubboInvoker#invoke(Invocation)` 方法，调用服务。代码如下：

```
/**
 * 使用的 {@link #clients} 的位置
 */
private final AtomicPositiveInteger index = new AtomicPositiveInteger();

  1: @Override
  2: protected Result doInvoke(final Invocation invocation) {
  3:     RpcInvocation inv = (RpcInvocation) invocation;
  4:     // 获得方法名
  5:     final String methodName = RpcUtils.getMethodName(invocation);
  6:     // 获得 `path`( 服务名 )，`version`
  7:     inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
  8:     inv.setAttachment(Constants.VERSION_KEY, version);
  9: 
 10:     // 获得 ExchangeClient 对象
 11:     ExchangeClient currentClient;
 12:     if (clients.length == 1) {
 13:         currentClient = clients[0];
 14:     } else {
 15:         currentClient = clients[index.getAndIncrement() % clients.length];
 16:     }
 17:     // 远程调用
 18:     try {
 19:         // 获得是否异步调用
 20:         boolean isAsync = RpcUtils.isAsync(getUrl(), invocation);
 21:         // 获得是否单向调用
 22:         boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
 23:         // 获得超时时间
 24:         int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
 25:         // 单向调用
 26:         if (isOneway) {
 27:             boolean isSent = getUrl().getMethodParameter(methodName, Constants.SENT_KEY, false);
 28:             currentClient.send(inv, isSent);
 29:             RpcContext.getContext().setFuture(null);
 30:             return new RpcResult();
 31:         // 异步调用
 32:         } else if (isAsync) {
 33:             ResponseFuture future = currentClient.request(inv, timeout);
 34:             RpcContext.getContext().setFuture(new FutureAdapter<Object>(future));
 35:             return new RpcResult();
 36:         // 同步调用
 37:         } else {
 38:             RpcContext.getContext().setFuture(null);
 39:             return (Result) currentClient.request(inv, timeout).get();
 40:         }
 41:     } catch (TimeoutException e) {
 42:         throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
 43:     } catch (RemotingException e) {
 44:         throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
 45:     }
 46: }
```

- 第 5 行：调用 `RpcUtils#getMethodName()` 方法，获得方法名。代码如下：

  ```
  public static String getMethodName(Invocation invocation) {
      // 泛化调用，第一个参数为方法名
      if (Constants.$INVOKE.equals(invocation.getMethodName())
              && invocation.getArguments() != null
              && invocation.getArguments().length > 0
              && invocation.getArguments()[0] instanceof String) {
          return (String) invocation.getArguments()[0];
      }
      // 普通调用，直接获得
      return invocation.getMethodName();
  }
  ```

- 第 6 至 8 行：获得 `path`( 服务名 )、`version` 。

- 第 10 至 16 行：**顺序**，获得 ExchangeClient 对象。

- 第 20 行：调用 `RpcUtils#isAsync(url, invocation)` 方法，判断是否异步调用。代码如下：

  ```
  public static boolean isAsync(URL url, Invocation inv) {
      return Boolean.TRUE.toString().equals(inv.getAttachment(Constants.ASYNC_KEY)) // RpcContext#asyncCall(Callable) 方法，可以设置
              || url.getMethodParameter(getMethodName(inv), Constants.ASYNC_KEY, false);
  }
  ```

  - 获得是否异步。服务引用或方法，任一配置 `async = true` ，即为异步。

- 第 22 行：调用 `RpcUtils#isOneway(url, invocation)` 方法，判断是否异步调用。代码如下：

  ```
  public static boolean isOneway(URL url, Invocation inv) {
      return Boolean.FALSE.toString().equals(inv.getAttachment(Constants.RETURN_KEY)) // RpcContext#asyncCall(Runnable) 方法，可以设置
              || !url.getMethodParameter(getMethodName(inv), Constants.RETURN_KEY, true);
  }
  ```

  - 获得是否单向。方法配置 `return = true` ，即为单向。

- 第 24 行：调用 `URL#getMethodParameter(method, key, defaultValue)` 方法，获得远程调用超时时间，单位：毫秒。

- 第 25 至 30 行：oneway 单向调用。

  - 第 28 行：**注意**，调用的是 `ExchangeClient#send(invocation, sent)` 方法，发送**消息**，而不是**请求**。
  - 第 29 行：设置 `RpcContext.future = null` ，无需 FutureFilter ，异步回调。
  - 第 30 行：创建 RpcResult 对象，**空返回**。

- 第 31 至 35 行：async 异步调用。

  - 第 33 行：调用 `ExchangeClient#request(invocation, timeout)` 方法，发送**请求**。
  - 第 34 行：调用 `RpcContext#setFuture(future)` 方法，在 FutureFitler 中，异步回调。
  - 第 35 行：创建 RpcResult 对象，**空返回**。

- 第 36 至 40 行：sync 同步调用。

  - 第 38 行：设置 `RpcContext.future = null` ，无需 FutureFilter ，异步回调。
  - 第 39 行：调用 `ExchangeClient#request(invocation, timeout)` 方法，发送**请求**。
  - 第 39 行：调用 `ResponseFuture#get()` 方法，**阻塞**等待，返回结果。

# 4. 提供者提供服务

在 DubboProtocol 类中，实现了自己的 ExchangeHandler 对象，处理请求、消息、连接、断开连接等事件。对于服务消费者的远程调用，通过 `#reply(ExchangeChannel channel, Object message)` 和 `#reply(Channel channel, Object message)` 方法来处理。如下图所示：![ExchangeHandler](http://www.iocoder.cn/images/Dubbo/2018_10_04/02_04.png)

下面，我们来看看每个方法的实现代码。

## 4.1 reply

```
 1: @Override
 2: public Object reply(ExchangeChannel channel, Object message) throws RemotingException {
 3:     if (message instanceof Invocation) {
 4:         Invocation inv = (Invocation) message;
 5:         // 获得请求对应的 Invoker 对象
 6:         Invoker<?> invoker = getInvoker(channel, inv);
 7:         // 如果是callback 需要处理高版本调用低版本的问题
 8:         // need to consider backward-compatibility if it's a callback
 9:         if (Boolean.TRUE.toString().equals(inv.getAttachments().get(IS_CALLBACK_SERVICE_INVOKE))) {
10:             String methodsStr = invoker.getUrl().getParameters().get("methods");
11:             boolean hasMethod = false;
12:             if (methodsStr == null || !methodsStr.contains(",")) {
13:                 hasMethod = inv.getMethodName().equals(methodsStr);
14:             } else {
15:                 String[] methods = methodsStr.split(",");
16:                 for (String method : methods) {
17:                     if (inv.getMethodName().equals(method)) {
18:                         hasMethod = true;
19:                         break;
20:                     }
21:                 }
22:             }
23:             if (!hasMethod) {
24:                 logger.warn(new IllegalStateException("The methodName " + inv.getMethodName() + " not found in callback service interface ,invoke will be ignored. please update the api interface. url is:" + invoker.getUrl()) + " ,invocation is :" + inv);
25:                 return null;
26:             }
27:         }
28:         // 设置调用方的地址
29:         RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());
30:         // 执行调用
31:         return invoker.invoke(inv);
32:     }
33:     throw new RemotingException(channel, message.getClass().getName() + ": " + message
34:             + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
35: }
```

- 用于处理服务消费者的同步调用和异步调用的请求。

- 第 6 行：调用 `#getInvoker(channel, invocation)` 方法，获得请求对应的 Invoker 对象。代码如下：

  ```
  /**
   * Exporter 集合
   *
   * key: 服务键 {@link #serviceKey(URL)} 或 {@link URL#getServiceKey()} 。
   *      不同协议会不同
   */
  protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>(); // FROM 父类 AbstractProtocol.java
  
    1: Invoker<?> getInvoker(Channel channel, Invocation inv) throws RemotingException {
    2:     boolean isCallBackServiceInvoke;
    3:     boolean isStubServiceInvoke;
    4:     int port = channel.getLocalAddress().getPort();
    5:     String path = inv.getAttachments().get(Constants.PATH_KEY);
    6:     // TODO 【8033 参数回调】
    7:     // if it's callback service on client side
    8:     isStubServiceInvoke = Boolean.TRUE.toString().equals(inv.getAttachments().get(Constants.STUB_EVENT_KEY));
    9:     if (isStubServiceInvoke) {
   10:         port = channel.getRemoteAddress().getPort();
   11:     }
   12:     // 如果是参数回调，获得真正的服务名 `path` 。
   13:     // callback
   14:     isCallBackServiceInvoke = isClientSide(channel) && !isStubServiceInvoke;
   15:     if (isCallBackServiceInvoke) {
   16:         path = inv.getAttachments().get(Constants.PATH_KEY) + "." + inv.getAttachments().get(Constants.CALLBACK_SERVICE_KEY);
   17:         inv.getAttachments().put(IS_CALLBACK_SERVICE_INVOKE, Boolean.TRUE.toString());
   18:     }
   19:     // 获得服务建
   20:     String serviceKey = serviceKey(port, path, inv.getAttachments().get(Constants.VERSION_KEY), inv.getAttachments().get(Constants.GROUP_KEY));
   21:     // 获得 Exporter 对象
   22:     DubboExporter<?> exporter = (DubboExporter<?>) exporterMap.get(serviceKey);
   23:     // 获得 Invoker 对象
   24:     if (exporter == null) {
   25:         throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + inv);
   26:     }
   27:     return exporter.getInvoker();
   28: }
  ```

  - 第 6 至 11 行：TODO 【8033 参数回调】

  - 第 12 至 18 行：如果是参数回调，获得真正的服务名 `path` 。在**参数回调**一文中，我们详细解析。

  - 第 20 行：调用 `#serviceKey(port, path, version)` 方法，获得服务键。代码如下：

    ```
    protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
    }
    ```

  - 第 22 行：从 `exporterMap` 集合中，获得 Exporter 对象。

  - 第 23 至 27 行：获得 Invoker 对象。

- 第 8 至 27 行：如果是**参数回调**，校验服务消费者实际存在对应的回调方法，通过方法名判断。

- 第 29 行：设置调用方的地址。

- 第 31 行：调用 `Invoker#invoke(invocation)` 方法，执行调用，并返回结果。后续的逻辑，和 `injvm://` 协议是一致的。

## 4.2 received

```
@Override
public void received(Channel channel, Object message) throws RemotingException {
    if (message instanceof Invocation) {
        this.reply((ExchangeChannel) channel, message);
    } else {
        super.received(channel, message);
    }
}
```

- 用于处理服务消费者的**单次调用**的消息，通过判断消息类型是不是 Invocation 。

## 4.3 connected && disconnected

> 本小节和 Dubbo RPC 无关系，只是为了完整分享 DubboProtocol ExchangeHandler 的完整代码实现。

在服务提供者上，有 `"onconnect"` 和 `"ondisconnect"` 配置项，在服务提供者连接或断开连接时，调用 Service 对应的方法。目前这个配置项，在 Dubbo 文档里，暂未提及。当然，这个在实际场景下，基本没用过。

```
@Override
public void connected(Channel channel) {
    this.invoke(channel, Constants.ON_CONNECT_KEY);
}

@Override
public void disconnected(Channel channel) throws RemotingException {
    if (logger.isInfoEnabled()) {
        logger.info("disconected from " + channel.getRemoteAddress() + ",url:" + channel.getUrl());
    }
    this.invoke(channel, Constants.ON_DISCONNECT_KEY);
}
```

- 调用 `#invoke(channel, methodKey)` 方法，执行对应的方法。代码如下：

  ```
  /**
   * 调用方法
   *
   * @param channel 通道
   * @param methodKey 方法名
   */
  private void invoke(Channel channel, String methodKey) {
      // 创建 Invocation 对象
      Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
      // 调用 received 方法，执行对应的方法
      if (invocation != null) {
          try {
              this.received(channel, invocation);
          } catch (Throwable t) {
              logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
          }
      }
  }
  
  private Invocation createInvocation(Channel channel, URL url, String methodKey) {
      String method = url.getParameter(methodKey);
      if (method == null || method.length() == 0) {
          return null;
      }
      RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
      invocation.setAttachment(Constants.PATH_KEY, url.getPath());
      invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
      invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
      invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
      if (url.getParameter(Constants.STUB_EVENT_KEY, false)) {
          invocation.setAttachment(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString());
      }
      return invocation;
  }
  ```

# 666. 彩蛋

![知识星球](http://www.iocoder.cn/images/Architecture/2017_12_29/01.png)

抽丝剥茧，像拨洋葱一样，一层一层一层，泪流满面。哈哈哈

清明节，扫代码第二波。