package com.joey.rpc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/19.
 */
public class RpcClientProxy {

    public <T> T clientProxy(final Class<T> interfaceCls , String host , int port){

        //使用动态代理
        return (T) Proxy.newProxyInstance(interfaceCls.getClassLoader(),
                new Class[]{interfaceCls}, new RemoteInvocationHandler(host , port));

    }
}
