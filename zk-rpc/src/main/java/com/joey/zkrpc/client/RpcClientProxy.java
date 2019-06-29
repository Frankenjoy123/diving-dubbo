package com.joey.zkrpc.client;

import com.joey.zkrpc.client.reg.IServiceDiscovery;

import java.lang.reflect.Proxy;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/19.
 */
public class RpcClientProxy {

    private IServiceDiscovery serviceDiscovery;

    public RpcClientProxy(IServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    public <T> T clientProxy(final Class<T> interfaceCls){

        //使用动态代理
        return (T) Proxy.newProxyInstance(interfaceCls.getClassLoader(),
                new Class[]{interfaceCls}, new RemoteInvocationHandler(serviceDiscovery));

    }
}
