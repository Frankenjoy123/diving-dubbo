# 精尽 Dubbo 源码分析 —— 服务调用（三）之远程调用（Dubbo）【3】异步调用



# 1. 概述

本文分享 `dubbo://` 协议的远程调用的**第三部分：异步调用**。

对应 [《Dubbo 用户指南 —— 事件通知》](http://dubbo.apache.org/zh-cn/docs/user/demos/events-notify.html) 文档。定义如下：

> 在调用之前、调用之后、出现异常时，会触发 `oninvoke`、`onreturn`、`onthrow` 三个事件，可以配置当事件发生时，通知哪个类的哪个方法。

看完定义，是不是有点疑惑，和本文的标题仿佛有些出入？相信自己，你是对的，标题是不严谨的，“错误”点如下：

- `oninvoke` 配置项，设置服务消费者**调用**服务提供者**之前**，执行前置方法，类似 AOP 的 `#beforeMethod(...)` 方法。
- `onreturn` 和 `onthrow` 配置项，设置服务消费者**调用**服务提供者**之后**，执行后置方法，类似 AOP 的 `#afterMethod(...)` 方法。有一点我们需要注意，一开始笔者理解错了，并非只有 `async = true` ，异步调用才支持回调，同步调用和单向调用也支持回调。

具体的调用，在 [《精尽 Dubbo 源码分析 —— 服务调用（二）之远程调用（Dubbo）【2】同步调用》「3. 消费者调用服务」](http://svip.iocoder.cn/Dubbo/rpc-dubbo-2-sync/?self)中，我们已经看到调用的代码。如果胖友没看过，建议先去看看。

# 2. FutureAdapter

[`com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter`](http://svip.iocoder.cn/Dubbo/rpc-dubbo-3-async/todo) ，实现 Future 接口，适配 ResponseFuture 。通过这样的方式，对上层调用方，**透明**化 ResponseFuture 的存在。代码如下：

```
public class FutureAdapter<V> implements Future<V> {

    private final ResponseFuture future;

    public FutureAdapter(ResponseFuture future) {
        this.future = future;
    }

    public ResponseFuture getFuture() {
        return future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        try {
            return (V) (((Result) future.get()).recreate());
        } catch (RemotingException e) {
            throw new ExecutionException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        int timeoutInMillis = (int) unit.convert(timeout, TimeUnit.MILLISECONDS);
        try {
            return (V) (((Result) future.get(timeoutInMillis)).recreate());
        } catch (com.alibaba.dubbo.remoting.TimeoutException e) {
            throw new TimeoutException(StringUtils.toString(e));
        } catch (RemotingException e) {
            throw new ExecutionException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

}
```

# 3. FutureFilter

[`com.alibaba.dubbo.rpc.protocol.dubbo.filte.FutureFilter`](https://github.com/YunaiV/dubbo/blob/master/dubbo-rpc/dubbo-rpc-default/src/main/java/com/alibaba/dubbo/rpc/protocol/dubbo/filter/FutureFilter.java) ，实现 Filter 接口，事件通知过滤器。实现代码如下：

```
 1: @Activate(group = Constants.CONSUMER)
 2: public class FutureFilter implements Filter {
 3: 
 4:     @Override
 5:     public Result invoke(final Invoker<?> invoker, final Invocation invocation) throws RpcException {
 6:         // 获得是否异步调用
 7:         final boolean isAsync = RpcUtils.isAsync(invoker.getUrl(), invocation);
 8: 
 9:         // 触发前置方法
10:         fireInvokeCallback(invoker, invocation);
11:         // need to configure if there's return value before the invocation in order to help invoker to judge if it's
12:         // necessary to return future.
13:         // 调用方法
14:         Result result = invoker.invoke(invocation);
15: 
16:         // 触发回调方法
17:         if (isAsync) { // 异步回调
18:             asyncCallback(invoker, invocation);
19:         } else { // 同步回调
20:             syncCallback(invoker, invocation, result);
21:         }
22:         return result;
23:     }
24:     
25:     // ... 省略部分方法
26: }
```

- `@Activate(group = Constants.CONSUMER)` 注解，基于 Dubbo SPI Activate 机制，只有**服务消费者**才生效该过滤器。
- 第 7 行：调用 `RpcUtils#isAsync(url, invocation)` 方法，判断是否异步调用。
- 第 10 行：调用 `#fireInvokeCallback(invoker, invocation)` 方法，触发前置方法。
- 第 14 行：调用 `invoker#invoke(invocation)` 方法，调用**服务提供者**，即 Dubbo RPC 。
- 第 16 至 21 行：触发回调方法。
  - 第 17 至 18 行：若**是**异步调用，调用 `#asyncCallback(invoker, invocation)` 方法，执行异步回调。
  - 第 19 至 21 行：若**非**异步调用，调用 `#syncCallback(invoker, invocation)` 方法，执行同步回调。
- 第 22 行：返回结果。如果是异步调用或单向调用，所以返回结果是**空**的。

## 3.1 fireInvokeCallback

```
 1: /**
 2:  * 触发前置方法
 3:  *
 4:  * @param invoker Invoker 对象
 5:  * @param invocation Invocation 对象
 6:  */
 7: private void fireInvokeCallback(final Invoker<?> invoker, final Invocation invocation) {
 8:     // 获得前置方法和对象
 9:     final Method onInvokeMethod = (Method) StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), invocation.getMethodName(), Constants.ON_INVOKE_METHOD_KEY));
10:     final Object onInvokeInst = StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), invocation.getMethodName(), Constants.ON_INVOKE_INSTANCE_KEY));
11:     if (onInvokeMethod == null && onInvokeInst == null) {
12:         return;
13:     }
14:     if (onInvokeMethod == null || onInvokeInst == null) {
15:         throw new IllegalStateException("service:" + invoker.getUrl().getServiceKey() + " has a onreturn callback config , but no such " + (onInvokeMethod == null ? "method" : "instance") + " found. url:" + invoker.getUrl());
16:     }
17:     if (!onInvokeMethod.isAccessible()) {
18:         onInvokeMethod.setAccessible(true);
19:     }
20: 
21:     // 调用前置方法
22:     Object[] params = invocation.getArguments();
23:     try {
24:         onInvokeMethod.invoke(onInvokeInst, params);
25:     } catch (InvocationTargetException e) {
26:         fireThrowCallback(invoker, invocation, e.getTargetException());
27:     } catch (Throwable e) {
28:         fireThrowCallback(invoker, invocation, e);
29:     }
30: }
```

- 第 8 至 19 行：获得前置方法和对象。StaticContext 在 [《精尽 Dubbo 源码分析 —— API 配置（三）之服务消费者》](http://svip.iocoder.cn/Dubbo/configuration-api-3/?self) 中，已经详细解析。
- 第 21 至 29 行：**反射**调用前置方法。

## 3.2 syncCallback

```
/**
 * 同步回调
 *
 * @param invoker Invoker 对象
 * @param invocation Invocation 对象
 * @param result RPC 结果
 */
private void syncCallback(final Invoker<?> invoker, final Invocation invocation, final Result result) {
    if (result.hasException()) { // 异常，触发异常回调
        fireThrowCallback(invoker, invocation, result.getException());
    } else { // 正常，触发正常回调
        fireReturnCallback(invoker, invocation, result.getValue());
    }
}
```

- `#fireThrowCallback(invoker, invocation, exception)` 方法，触发异常回调方法，代码如下：

  ```
  private void fireReturnCallback(final Invoker<?> invoker, final Invocation invocation, final Object result) {
      // 获得 `onreturn` 方法和对象
      final Method onReturnMethod = (Method) StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), invocation.getMethodName(), Constants.ON_RETURN_METHOD_KEY));
      final Object onReturnInst = StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), invocation.getMethodName(), Constants.ON_RETURN_INSTANCE_KEY));
      //not set onreturn callback
      if (onReturnMethod == null && onReturnInst == null) {
          return;
      }
      if (onReturnMethod == null || onReturnInst == null) {
          throw new IllegalStateException("service:" + invoker.getUrl().getServiceKey() + " has a onreturn callback config , but no such " + (onReturnMethod == null ? "method" : "instance") + " found. url:" + invoker.getUrl());
      }
      if (!onReturnMethod.isAccessible()) {
          onReturnMethod.setAccessible(true);
      }
  
      // 参数数组
      Object[] args = invocation.getArguments();
      Object[] params;
      Class<?>[] rParaTypes = onReturnMethod.getParameterTypes();
      if (rParaTypes.length > 1) {
          if (rParaTypes.length == 2 && rParaTypes[1].isAssignableFrom(Object[].class)) {
              params = new Object[2];
              params[0] = result;
              params[1] = args;
          } else {
              params = new Object[args.length + 1];
              params[0] = result;
              System.arraycopy(args, 0, params, 1, args.length);
          }
      } else {
          params = new Object[]{result};
      }
  
      // 调用方法
      try {
          onReturnMethod.invoke(onReturnInst, params);
      } catch (InvocationTargetException e) {
          fireThrowCallback(invoker, invocation, e.getTargetException());
      } catch (Throwable e) {
          fireThrowCallback(invoker, invocation, e);
      }
  }
  ```

- `#fireReturnCallback(invoker, invocation, result)` 方法，触发正常回调方法，代码如下：

  ```
  private void fireThrowCallback(final Invoker<?> invoker, final Invocation invocation, final Throwable exception) {
      // 获得 `onthrow` 方法和对象
      final Method onthrowMethod = (Method) StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), invocation.getMethodName(), Constants.ON_THROW_METHOD_KEY));
      final Object onthrowInst = StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), invocation.getMethodName(), Constants.ON_THROW_INSTANCE_KEY));
      // onthrow callback not configured
      if (onthrowMethod == null && onthrowInst == null) {
          return;
      }
      if (onthrowMethod == null || onthrowInst == null) {
          throw new IllegalStateException("service:" + invoker.getUrl().getServiceKey() + " has a onthrow callback config , but no such " + (onthrowMethod == null ? "method" : "instance") + " found. url:" + invoker.getUrl());
      }
      if (!onthrowMethod.isAccessible()) {
          onthrowMethod.setAccessible(true);
      }
  
      Class<?>[] rParaTypes = onthrowMethod.getParameterTypes();
      if (rParaTypes[0].isAssignableFrom(exception.getClass())) { // 符合异常
          try {
              // 参数数组
              Object[] args = invocation.getArguments();
              Object[] params;
              if (rParaTypes.length > 1) {
                  if (rParaTypes.length == 2 && rParaTypes[1].isAssignableFrom(Object[].class)) {
                      params = new Object[2];
                      params[0] = exception;
                      params[1] = args;
                  } else {
                      params = new Object[args.length + 1];
                      params[0] = exception;
                      System.arraycopy(args, 0, params, 1, args.length);
                  }
              } else {
                  params = new Object[]{exception};
              }
  
              // 调用方法
              onthrowMethod.invoke(onthrowInst, params);
          } catch (Throwable e) {
              logger.error(invocation.getMethodName() + ".call back method invoke error . callback method :" + onthrowMethod + ", url:" + invoker.getUrl(), e);
          }
      } else { // 不符合异常，打印错误日志
          logger.error(invocation.getMethodName() + ".call back method invoke error . callback method :" + onthrowMethod + ", url:" + invoker.getUrl(), exception);
      }
  }
  ```

## 3.3 asyncCallback

```
/**
 * 异步回调
 *
 * @param invoker Invoker 对象
 * @param invocation Invocation 对象
 */
private void asyncCallback(final Invoker<?> invoker, final Invocation invocation) {
    // 获得 Future 对象
    Future<?> f = RpcContext.getContext().getFuture();
    if (f instanceof FutureAdapter) {
        ResponseFuture future = ((FutureAdapter<?>) f).getFuture();
        // 触发回调
        future.setCallback(new ResponseCallback() {

            /**
             * 触发正常回调方法
             *
             * @param rpcResult RPC 结果
             */
            public void done(Object rpcResult) {
                if (rpcResult == null) {
                    logger.error(new IllegalStateException("invalid result value : null, expected " + Result.class.getName()));
                    return;
                }
                // must be rpcResult
                if (!(rpcResult instanceof Result)) {
                    logger.error(new IllegalStateException("invalid result type :" + rpcResult.getClass() + ", expected " + Result.class.getName()));
                    return;
                }
                Result result = (Result) rpcResult;
                if (result.hasException()) { // 触发正常回调方法
                    fireThrowCallback(invoker, invocation, result.getException());
                } else { // 触发异常回调方法
                    fireReturnCallback(invoker, invocation, result.getValue());
                }
            }

            /**
             * 触发异常回调方法
             *
             * @param exception 异常
             */
            public void caught(Throwable exception) {
                fireThrowCallback(invoker, invocation, exception);
            }
        });
    }
}
```

# 666. 彩蛋

![知识星球](http://www.iocoder.cn/images/Architecture/2017_12_29/01.png)

实现比较简单，🙂 貌似把所有代码贴了一遍。

清明节，扫代码第三波。