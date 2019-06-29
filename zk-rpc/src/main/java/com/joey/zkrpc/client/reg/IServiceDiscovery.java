package com.joey.zkrpc.client.reg;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/21.
 */
public interface IServiceDiscovery {

    String getAddress(String serviceName);

}
