package com.joey.zkrpc.client.reg;

import com.joey.zkrpc.client.loadbalance.AbstractLoadBalance;
import com.joey.zkrpc.common.ConfigConstants;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;

import java.io.IOException;
import java.util.List;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/21.
 */
public class ServiceDiscoveryImpl implements IServiceDiscovery{

    private String connectString;

    CuratorFramework curatorFramework;

    AbstractLoadBalance loadBalance;

    public ServiceDiscoveryImpl(String connectString , AbstractLoadBalance loadBalance) {
        this.connectString = connectString;
        this.loadBalance = loadBalance;

        curatorFramework = null;

        try {
            curatorFramework = CuratorFrameworkFactory.builder()
                    .connectString(ConfigConstants.connectString)
                    .connectionTimeoutMs(4000)
                    .retryPolicy(new ExponentialBackoffRetry(1000,10))
                    .build();
            curatorFramework.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public String getAddress(String serviceName) {

        String servicePath = ConfigConstants.registerRootPath + "/" + serviceName;

        try {
            List<String> list = curatorFramework.getChildren().forPath(servicePath);

            return loadBalance.loadSelect(list);

        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }
}
