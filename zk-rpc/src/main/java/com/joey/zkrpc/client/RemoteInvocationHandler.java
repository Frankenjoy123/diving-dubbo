package com.joey.zkrpc.client;


import com.joey.zkrpc.RpcRequest;
import com.joey.zkrpc.client.reg.IServiceDiscovery;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/19.
 */
public class RemoteInvocationHandler implements InvocationHandler{



    private IServiceDiscovery serviceDiscovery;

    public RemoteInvocationHandler(IServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }



    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {


        String serviceName = method.getDeclaringClass().getName();
        String address = serviceDiscovery.getAddress(serviceName);

        String[] arr = address.split(":");
        String host = arr[0];
        int port = Integer.parseInt(arr[1]);

        TCPTransport tcpTransport = new TCPTransport(host, port);

        RpcRequest request = new RpcRequest();
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParams(args);

        Object object = tcpTransport.send(request);
        return object;
    }
}
