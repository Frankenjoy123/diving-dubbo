# 精尽 Dubbo 源码分析 —— 过滤器（七）之 ExceptionFilter



# 1. 概述

本文分享异常过滤器 ExceptionFilter ，用于服务**提供者**中。用途如下：

> FROM ExceptionFilter 上的注释：
>
> 1. 不期望的异常打 ERROR 日志( Provider端 )。不期望的日志即是，没有的接口上声明的Unchecked异常。
> 2. 异常不在 API 包中，则 Wrap 一层 RuntimeException 。RPC 对于第一层异常会直接序列化传输( Cause 异常会 String 化) ，避免异常在 Client 出不能**反序列化**问题。

🙂 和我们平时业务写的用于**捕捉异常**的过滤器或者拦截器**不太一样**，而是关注点在服务消费者会不会出现不存在该异常类，导致反序列化的问题。

# 2. ExceptionFilter

`com.alibaba.dubbo.rpc.filter.ExceptionFilter` ，实现 Filter 接口，异常过滤器实现类。代码如下：

```
 1: @Activate(group = Constants.PROVIDER)
 2: public class ExceptionFilter implements Filter {
 3: 
 4:     private final Logger logger;
 5: 
 6:     public ExceptionFilter() {
 7:         this(LoggerFactory.getLogger(ExceptionFilter.class));
 8:     }
 9: 
10:     public ExceptionFilter(Logger logger) {
11:         this.logger = logger;
12:     }
13: 
14:     @Override
15:     public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
16:         try {
17:             // 服务调用
18:             Result result = invoker.invoke(invocation);
19:             // 有异常，并且非泛化调用
20:             if (result.hasException() && GenericService.class != invoker.getInterface()) {
21:                 try {
22:                     Throwable exception = result.getException();
23: 
24:                     // directly throw if it's checked exception
25:                     // 如果是checked异常，直接抛出
26:                     if (!(exception instanceof RuntimeException) && (exception instanceof Exception)) {
27:                         return result;
28:                     }
29:                     // directly throw if the exception appears in the signature
30:                     // 在方法签名上有声明，直接抛出
31:                     try {
32:                         Method method = invoker.getInterface().getMethod(invocation.getMethodName(), invocation.getParameterTypes());
33:                         Class<?>[] exceptionClassses = method.getExceptionTypes();
34:                         for (Class<?> exceptionClass : exceptionClassses) {
35:                             if (exception.getClass().equals(exceptionClass)) {
36:                                 return result;
37:                             }
38:                         }
39:                     } catch (NoSuchMethodException e) {
40:                         return result;
41:                     }
42: 
43:                     // 未在方法签名上定义的异常，在服务器端打印 ERROR 日志
44:                     // for the exception not found in method's signature, print ERROR message in server's log.
45:                     logger.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()
46:                             + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()
47:                             + ", exception: " + exception.getClass().getName() + ": " + exception.getMessage(), exception);
48: 
49:                     // 异常类和接口类在同一 jar 包里，直接抛出
50:                     // directly throw if exception class and interface class are in the same jar file.
51:                     String serviceFile = ReflectUtils.getCodeBase(invoker.getInterface());
52:                     String exceptionFile = ReflectUtils.getCodeBase(exception.getClass());
53:                     if (serviceFile == null || exceptionFile == null || serviceFile.equals(exceptionFile)) {
54:                         return result;
55:                     }
56:                     // 是JDK自带的异常，直接抛出
57:                     // directly throw if it's JDK exception
58:                     String className = exception.getClass().getName();
59:                     if (className.startsWith("java.") || className.startsWith("javax.")) {
60:                         return result;
61:                     }
62:                     // 是Dubbo本身的异常，直接抛出
63:                     // directly throw if it's dubbo exception
64:                     if (exception instanceof RpcException) {
65:                         return result;
66:                     }
67: 
68:                     // 否则，包装成RuntimeException抛给客户端
69:                     // otherwise, wrap with RuntimeException and throw back to the client
70:                     return new RpcResult(new RuntimeException(StringUtils.toString(exception)));
71:                 } catch (Throwable e) {
72:                     logger.warn("Fail to ExceptionFilter when called by " + RpcContext.getContext().getRemoteHost()
73:                             + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()
74:                             + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
75:                     return result;
76:                 }
77:             }
78:             // 返回
79:             return result;
80:         } catch (RuntimeException e) {
81:             logger.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()
82:                     + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()
83:                     + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
84:             throw e;
85:         }
86:     }
87: 
88: }
```

- 第 18 行：调用 `Invoker#invoke(invocation)` 方法，服务调用。

- 第 21 行：调用结果有异常，并且非**泛化调用**。

- 第 24 至 28 行：如果是 checked 异常，直接返回。因为，checked 异常，肯定定义在接口上了。

- 第 29 至 41 行：在接口方法的签名有生命，直接返回结果。

- 第 45 至 47 行：未在方法签名上定义的异常，在服务器端打印 ERROR 日志。

- 第 49 至 55 行：异常类和接口类在同一 **jar** 包里，直接返回结果。因为，服务消费者可以反序列化该异常。代码如下：

  ```
  public static String getCodeBase(Class<?> cls) {
      if (cls == null) {
          return null;
      }
      ProtectionDomain domain = cls.getProtectionDomain();
      if (domain == null) {
          return null;
      }
      CodeSource source = domain.getCodeSource();
      if (source == null) {
          return null;
      }
      URL location = source.getLocation();
      if (location == null) {
          return null;
      }
      return location.getFile();
  }
  ```

- 第 56 至 61 行：是 JDK 自带的异常，直接返回结果。

- 第 62 至 66 行：是 Dubbo 本身的异常，直接返回结果。

- 第 70 行：否则，包装成 RuntimeException 异常返回给服务消费者，同时把异常堆栈给包进去。代码如下：

  ```
  public static String toString(Throwable e) {
      UnsafeStringWriter w = new UnsafeStringWriter();
      PrintWriter p = new PrintWriter(w);
      p.print(e.getClass().getName());
      if (e.getMessage() != null) {
          p.print(": " + e.getMessage());
      }
      p.println();
      try {
          e.printStackTrace(p);
          return w.toString();
      } finally {
          p.close();
      }
  }
  ```

# 666. 彩蛋

😜 一开始想错了，怪不得觉得好奇怪。

另外，笔者有个想法。我们在实际使用时，可能会定义通用的 BusinessException ，并且每个接口，实际都会抛出该异常。那么要求开发每个接口都定义抛出 BusinessException 是比较“麻烦”的。
但是，按照 ExceptionFilter 的逻辑，会打印异常日志。
所以，笔者的想法是，重写 ExceptionFilter ，定义一些通用异常，允许直接返回结果。🙂

- 墙裂推荐 [《Dubbo(四) 异常处理》](https://blog.csdn.net/qq315737546/article/details/53915067)
- [《浅谈 Dubbo 的 ExceptionFilter 异常处理》](https://blog.csdn.net/mj158518/article/details/51228649)