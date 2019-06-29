package com.joey.zkrpc.server.zk;

import com.joey.zkrpc.common.ConfigConstants;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/20.
 */
public class RegisterCenterImpl implements IRegisterCenter{

    private CuratorFramework curatorFramework;

    {
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
    public void register(String serviceName, String serviceAddress) {

        String servicePath = ConfigConstants.registerRootPath + "/" + serviceName;


        try {
            if (curatorFramework.checkExists().forPath(servicePath) == null){
                curatorFramework.create().creatingParentsIfNeeded().forPath(servicePath,"0".getBytes());
            }


            String addressPath = servicePath + "/" + serviceAddress;
            String rs = curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                    .forPath(addressPath);

            System.out.println("注册服务成功 -> " + rs);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
