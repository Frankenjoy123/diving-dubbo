package com.joey.zkrpc.server.zk;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/20.
 */
public interface IRegisterCenter {

    /**
     * 注册服务名和服务地址
     * @param serviceName
     * @param serviceAddress
     */
    void register(String serviceName , String serviceAddress);
}
