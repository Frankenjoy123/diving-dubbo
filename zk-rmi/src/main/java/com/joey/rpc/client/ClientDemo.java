package com.joey.rpc.client;

import com.joey.rpc.ISportService;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
public class ClientDemo {

    public static void main(String[] args) {

        RpcClientProxy proxy = new RpcClientProxy();

        ISportService sportService = proxy.clientProxy(ISportService.class,"127.0.0.1",8888);

        String result = sportService.doSport("篮球");

        System.out.println(result);
    }
}
