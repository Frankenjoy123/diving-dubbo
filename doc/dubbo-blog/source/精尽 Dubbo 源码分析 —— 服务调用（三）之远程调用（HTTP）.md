# 精尽 Dubbo 源码分析 —— 服务调用（三）之远程调用（HTTP）



# 1. 概述

本文，我们分享 `http://` 协议的远程调用，主要分成**三个部分**：

- 服务暴露
- 服务引用
- 服务调用

对应项目为 `dubbo-rpc-http` 。

对应文档为 [《Dubbo 用户指南 —— http://》](http://dubbo.apache.org/zh-cn/docs/user/references/protocol/http.html) 。定义如下：

> 基于 HTTP 表单的远程调用协议，采用 Spring 的 **HttpInvoker** 实现

**注意**，从定义上我们可以看出，不是我们常规理解的 HTTP 调用，而是 **Spring 的 HttpInvoker** 。

本文涉及类图（红圈部分）如下：

![类图](http://www.iocoder.cn/images/Dubbo/2018_10_10/01.png)

# 2. AbstractProxyProtocol

[`com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol`](https://github.com/YunaiV/dubbo/blob/master/dubbo-rpc/dubbo-rpc-api/src/main/java/com/alibaba/dubbo/rpc/protocol/AbstractProxyProtocol.java) ，实现 AbstractProtocol 抽象类，**Proxy** 协议抽象类。为 HttpProtocol 、RestProtocol 等子类，提供公用的服务暴露、服务引用的**公用方法**，同时定义了如下**抽象方法**，用于不同子类协议实现类的**自定义**的逻辑：

```
/**
 * 执行暴露，并返回取消暴露的回调 Runnable
 *
 * @param impl 服务 Proxy 对象
 * @param type 服务接口
 * @param url URL
 * @param <T> 服务接口
 * @return 消暴露的回调 Runnable
 * @throws RpcException 当发生异常
 */
protected abstract <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException;

/**
 * 执行引用，并返回调用远程服务的 Service 对象
 *
 * @param type 服务接口
 * @param url URL
 * @param <T> 服务接口
 * @return 调用远程服务的 Service 对象
 * @throws RpcException 当发生异常
 */
protected abstract <T> T doRefer(Class<T> type, URL url) throws RpcException;
```

## 2.1 构造方法

```
/**
 * 需要抛出的异常类集合，详见 {@link #reder(Class, URL)} 方法。
 */
private final List<Class<?>> rpcExceptions = new CopyOnWriteArrayList<Class<?>>();
/**
 * ProxyFactory 对象
 */
private ProxyFactory proxyFactory;

public AbstractProxyProtocol() {
}

public AbstractProxyProtocol(Class<?>... exceptions) {
    for (Class<?> exception : exceptions) {
        addRpcException(exception);
    }
}

public void addRpcException(Class<?> exception) {
    this.rpcExceptions.add(exception);
}
```

- `rpcExceptions` 属性，不同协议的远程调用，会抛出的异常是不同的。在 `#refer(Class, URL)` 方法中，我们会看到对这个属性的使用，理解会更清晰一些。

## 2.2 export

```
/**
 * Exporter 集合
 *
 * key: 服务键 {@link #serviceKey(URL)} 或 {@link URL#getServiceKey()} 。
 *      不同协议会不同
 */
protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>(); // FROM AbstractProtocol.java

  1: @Override
  2: @SuppressWarnings("unchecked")
  3: public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
  4:     // 获得服务键
  5:     final String uri = serviceKey(invoker.getUrl());
  6:     // 获得 Exporter 对象。若已经暴露，直接返回。
  7:     Exporter<T> exporter = (Exporter<T>) exporterMap.get(uri);
  8:     if (exporter != null) {
  9:         return exporter;
 10:     }
 11:     // 执行暴露服务
 12:     final Runnable runnable = doExport(proxyFactory.getProxy(invoker), invoker.getInterface(), invoker.getUrl());
 13:     // 创建 Exporter 对象
 14:     exporter = new AbstractExporter<T>(invoker) {
 15: 
 16:         @Override
 17:         public void unexport() {
 18:             // 取消暴露
 19:             super.unexport();
 20:             exporterMap.remove(uri);
 21:             // 执行取消暴露的回调
 22:             if (runnable != null) {
 23:                 try {
 24:                     runnable.run();
 25:                 } catch (Throwable t) {
 26:                     logger.warn(t.getMessage(), t);
 27:                 }
 28:             }
 29:         }
 30: 
 31:     };
 32:     // 添加到 Exporter 集合
 33:     exporterMap.put(uri, exporter);
 34:     return exporter;
 35: }
```

- 第 5 行：调用 `#serviceKey(url)` 方法，获得服务键。代码如下：

  ```
  protected static String serviceKey(URL url) {
      return ProtocolUtils.serviceKey(url);
  }
  ```

- 第 6 至 10 行：从 `exporterMap` 中，获得 Exporter 对象。若已经暴露，直接返回。

- 第 12 行：调用 `ProxyFactory#getProxy(invoker)` 方法，获得 Service Proxy 对象。

- 第 12 行：调用 `#doExport(impl, type, url)` **抽象**方法，执行**子类实现**的暴露服务。

- 第 14 至 31 行：创建 Exporter 对象。基于 AbstractExporter 抽象类实现，覆写

   

  ```
  #unexport()
  ```

   

  方法，代码如下：

  - 第 18 至 20 行：取消暴露。
  - 第 22 至 28 行：调用 `Runnable#run()` 方法，执行取消暴露的回调方法。

- 第 33 行：添加到 Exporter 集合。

## 2.3 refer

```
/**
 * Invoker 集合
 */
//TODO SOFEREFENCE
protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<Invoker<?>>(); // FROM AbstractProtocol.java

  1: @Override
  2: public <T> Invoker<T> refer(final Class<T> type, final URL url) throws RpcException {
  3:     // 执行引用服务
  4:     final Invoker<T> target = proxyFactory.getInvoker(doRefer(type, url), type, url);
  5:     // 创建 Invoker 对象
  6:     Invoker<T> invoker = new AbstractInvoker<T>(type, url) {
  7: 
  8:         @Override
  9:         protected Result doInvoke(Invocation invocation) throws Throwable {
 10:             try {
 11:                 // 调用
 12:                 Result result = target.invoke(invocation);
 13:                 // 若返回结果带有异常，并且需要抛出，则抛出异常。
 14:                 Throwable e = result.getException();
 15:                 if (e != null) {
 16:                     for (Class<?> rpcException : rpcExceptions) {
 17:                         if (rpcException.isAssignableFrom(e.getClass())) {
 18:                             throw getRpcException(type, url, invocation, e);
 19:                         }
 20:                     }
 21:                 }
 22:                 return result;
 23:             } catch (RpcException e) {
 24:                 // 若是未知异常，获得异常对应的错误码
 25:                 if (e.getCode() == RpcException.UNKNOWN_EXCEPTION) {
 26:                     e.setCode(getErrorCode(e.getCause()));
 27:                 }
 28:                 throw e;
 29:             } catch (Throwable e) {
 30:                 // 抛出 RpcException 异常
 31:                 throw getRpcException(type, url, invocation, e);
 32:             }
 33:         }
 34: 
 35:     };
 36:     // 添加到 Invoker 集合。
 37:     invokers.add(invoker);
 38:     return invoker;
 39: }
```

- 第 4 行：调用 `#doRefer(type, url)` **抽象**方法，执行**子类实现**的引用服务。

- 第 4 行：调用 `ProxyFactory#getInvoker(proxy, type, url)` 方法，获得 Invoker 对象。

- 第 6 至 35 行：创建 Invoker 对象。基于 AbstractExporter 抽象类实现，覆写 `#doInvoke(invocation)` 方法，代码如下：

  - 第 12 行：调用 `Invoker#invoke(invocation)` 方法，执行 RPC 调用。

  - 第 13 至 21 行：若返回结果带有异常，并且需要抛出( 异常在 `rpcExceptions` 中)，则抛出异常。

  - 第 22 行：返回调用结果。

  - 第 23 至 28 行：若捕捉到 RpcException 异常，调用 `#getErrorCode(Throwable)` 方法，获得异常对应的错误码。代码如下：

    ```
    /**
     * 获得异常对应的错误码
     *
     * @param e 异常
     * @return 错误码
     */
    protected int getErrorCode(Throwable e) {
        return RpcException.UNKNOWN_EXCEPTION;
    }
    ```

    - 子类协议实现类，一般会覆写这个方法，实现自己异常的翻译。

  - 第 29 至 32 行：若捕捉到 Throwable 异常，调用 `#getRpcException(type, url, invocation, e)` 方法，包装成 RpcException 异常，代码如下：

    ```
    protected RpcException getRpcException(Class<?> type, URL url, Invocation invocation, Throwable e) {
        RpcException re = new RpcException("Failed to invoke remote service: " + type + ", method: "
                + invocation.getMethodName() + ", cause: " + e.getMessage(), e);
        re.setCode(getErrorCode(e));
        return re;
    }
    ```

- 第 37 行：添加到 Invoker 集合。

# 3. HttpProtocol

[`com.alibaba.dubbo.rpc.protocol.http.HttpProtocol`](https://github.com/YunaiV/dubbo/blob/master/dubbo-rpc/dubbo-rpc-http/src/main/java/com/alibaba/dubbo/rpc/protocol/http/HttpProtocol.java) ，实现 AbstractProxyProtocol 抽象类，`dubbo://` 协议实现类。

## 3.1 构造方法

```
/**
 * 默认服务器端口
 */
public static final int DEFAULT_PORT = 80;
/**
 * Http 服务器集合
 *
 * key：ip:port
 */
private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();
/**
 * Spring HttpInvokerServiceExporter 集合
 *
 * key：path 服务名
 */
private final Map<String, HttpInvokerServiceExporter> skeletonMap = new ConcurrentHashMap<String, HttpInvokerServiceExporter>();
/**
 * HttpBinder$Adaptive 对象
 */
private HttpBinder httpBinder;

public HttpProtocol() {
    super(RemoteAccessException.class);
}

public void setHttpBinder(HttpBinder httpBinder) {
    this.httpBinder = httpBinder;
}
```

- `serverMap` 属性，HttpServer 集合。键为 `ip:port` ，通过 `#getAddr(url)` 方法，计算。代码如下：

  ```
  // AbstractProxyProtocol.java
  protected String getAddr(URL url) {
      String bindIp = url.getParameter(Constants.BIND_IP_KEY, url.getHost());
      if (url.getParameter(Constants.ANYHOST_KEY, false)) {
          bindIp = Constants.ANYHOST_VALUE;
      }
      return NetUtils.getIpByHost(bindIp) + ":" + url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
  }
  ```

- `skeletonMap` 属性，Spring HttpInvokerServiceExporter 集合。请求处理过程为 `HttpServer => DispatcherServlet => InternalHandler => HttpInvokerServiceExporter` 。

- `httpBinder` 属性，HttpBinder$Adaptive 对象，通过 `#setHttpBinder(httpBinder)` 方法，Dubbo SPI 调用设置。

- `rpcExceptions = RemoteAccessException.class` 。

## 3.2 doExport

```
 1: @Override
 2: protected <T> Runnable doExport(final T impl, Class<T> type, URL url) throws RpcException {
 3:     // 获得服务器地址
 4:     String addr = getAddr(url);
 5:     // 获得 HttpServer 对象。若不存在，进行创建。
 6:     HttpServer server = serverMap.get(addr);
 7:     if (server == null) {
 8:         server = httpBinder.bind(url, new InternalHandler()); // InternalHandler
 9:         serverMap.put(addr, server);
10:     }
11:     // 创建 HttpInvokerServiceExporter 对象
12:     final HttpInvokerServiceExporter httpServiceExporter = new HttpInvokerServiceExporter();
13:     httpServiceExporter.setServiceInterface(type);
14:     httpServiceExporter.setService(impl);
15:     try {
16:         httpServiceExporter.afterPropertiesSet();
17:     } catch (Exception e) {
18:         throw new RpcException(e.getMessage(), e);
19:     }
20:     // 添加到 skeletonMap 中
21:     final String path = url.getAbsolutePath();
22:     skeletonMap.put(path, httpServiceExporter);
23:     // 返回取消暴露的回调 Runnable
24:     return new Runnable() {
25:         public void run() {
26:             skeletonMap.remove(path);
27:         }
28:     };
29: }
```

- 基于 `dubbo-remoting-http` 项目，作为**通信服务器**。
- 第 4 行：调用 `#getAddr(url)` 方法，获得服务器地址。
- 第 5 至 10 行：从 `serverMap` 中，获得 HttpServer 对象。若不存在，调用 `HttpBinder#bind(url, handler)` 方法，创建 HttpServer 对象。此处使用的 InternalHandler ，下文详细解析。
- 第 11 至 19 行：创建 HttpInvokerServiceExporter 对象。
- 第 20 至 22 行：添加到 `skeletonMap` 集合中。
- 第 23 至 28 行：返回取消暴露的回调 Runnable 对象。

### 3.2.1 InternalHandler

```
private class InternalHandler implements HttpHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String uri = request.getRequestURI();
        // 获得 HttpInvokerServiceExporter 对象
        HttpInvokerServiceExporter skeleton = skeletonMap.get(uri);
        // 必须是 POST 请求
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            response.setStatus(500);
        // 执行调用
        } else {
            RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
            try {
                skeleton.handleRequest(request, response);
            } catch (Throwable e) {
                throw new ServletException(e);
            }
        }
    }

}
```

## 3.3 doRefer

```
 1: @Override
 2: @SuppressWarnings("unchecked")
 3: protected <T> T doRefer(final Class<T> serviceType, final URL url) throws RpcException {
 4:     // 创建 HttpInvokerProxyFactoryBean 对象
 5:     final HttpInvokerProxyFactoryBean httpProxyFactoryBean = new HttpInvokerProxyFactoryBean();
 6:     httpProxyFactoryBean.setServiceUrl(url.toIdentityString());
 7:     httpProxyFactoryBean.setServiceInterface(serviceType);
 8:     // 创建执行器 SimpleHttpInvokerRequestExecutor 对象
 9:     String client = url.getParameter(Constants.CLIENT_KEY);
10:     if (client == null || client.length() == 0 || "simple".equals(client)) {
11:         SimpleHttpInvokerRequestExecutor httpInvokerRequestExecutor = new SimpleHttpInvokerRequestExecutor() {
12:             protected void prepareConnection(HttpURLConnection con,
13:                                              int contentLength) throws IOException {
14:                 super.prepareConnection(con, contentLength);
15:                 con.setReadTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
16:                 con.setConnectTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
17:             }
18:         };
19:         httpProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
20:     // 创建执行器 HttpComponentsHttpInvokerRequestExecutor 对象
21:     } else if ("commons".equals(client)) {
22:         HttpComponentsHttpInvokerRequestExecutor httpInvokerRequestExecutor = new HttpComponentsHttpInvokerRequestExecutor();
23:         httpInvokerRequestExecutor.setReadTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
24:         httpProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
25:     } else {
26:         throw new IllegalStateException("Unsupported http protocol client " + client + ", only supported: simple, commons");
27:     }
28:     httpProxyFactoryBean.afterPropertiesSet();
29:     // 返回 HttpInvokerProxyFactoryBean 对象
30:     return (T) httpProxyFactoryBean.getObject();
31: }
```

- 基于 HttpClient ，作为**通信客户端**。

- 第 4 至 7 行：创建 HttpInvokerProxyFactoryBean 对象。

- 第 9 至 27 行：获得

   

  ```
  client
  ```

   

  配置项，根据该配置项，创建对应的执行器。

  - `"simple"`：第 10 至 19 行：创建执行器 SimpleHttpInvokerRequestExecutor 对象。
  - `"commons"`：第 20 至 24 行：创建执行器 HttpComponentsHttpInvokerRequestExecutor 对象。
  - 两者的差异点在于使用的 HttpClient 不同，前者使用 JDK HttpClient ，后者使用 Apache HttpClient 。

- 第 30 行：返回 HttpInvokerProxyFactoryBean 对象。

- 🙂 具体 RPC 调用的实现，在父类 `#refer()` 方法里。

### 3.3.1 getErrorCode

```
@Override
@SuppressWarnings("Duplicates")
protected int getErrorCode(Throwable e) {
    if (e instanceof RemoteAccessException) {
        e = e.getCause();
    }
    if (e != null) {
        Class<?> cls = e.getClass();
        if (SocketTimeoutException.class.equals(cls)) {
            return RpcException.TIMEOUT_EXCEPTION;
        } else if (IOException.class.isAssignableFrom(cls)) {
            return RpcException.NETWORK_EXCEPTION;
        } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
            return RpcException.SERIALIZATION_EXCEPTION;
        }
    }
    return super.getErrorCode(e);
}
```

- 将异常，翻译成 Dubbo 异常码。

# 666. 彩蛋

![知识星球](http://www.iocoder.cn/images/Architecture/2017_12_29/01.png)

来自清明节，一边食物中毒，一边更新。s