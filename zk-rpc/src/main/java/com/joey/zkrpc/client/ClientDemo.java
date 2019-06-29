package com.joey.zkrpc.client;


import com.joey.zkrpc.ISportService;
import com.joey.zkrpc.client.loadbalance.RandomLoadBanlance;
import com.joey.zkrpc.client.reg.IServiceDiscovery;
import com.joey.zkrpc.client.reg.ServiceDiscoveryImpl;
import com.joey.zkrpc.common.ConfigConstants;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
public class ClientDemo {

    public static void main(String[] args) {

        IServiceDiscovery serviceDiscovery = new ServiceDiscoveryImpl(ConfigConstants.connectString , new RandomLoadBanlance());

        RpcClientProxy proxy = new RpcClientProxy(serviceDiscovery);

        ISportService sportService = proxy.clientProxy(ISportService.class);

        String result = sportService.doSport("篮球");

        System.out.println(result);
    }
}
