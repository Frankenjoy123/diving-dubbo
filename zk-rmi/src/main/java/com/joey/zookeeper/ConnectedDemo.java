package com.joey.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/10.
 */
public class ConnectedDemo {

    // 集群环境用,隔开
    private static final String CONNECTSTRING = "192.168.0.118:2181,192.168.0.119:2181,192.168.0.120:2181";
    private static ZooKeeper zookeeper;

    public static void main(String[] args) throws Exception
    {
        connect();

//        String createResult = zookeeper.create("/zxw" , "hello".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE
//                        ,CreateMode.PERSISTENT);
//        System.out.println("create result : " + createResult);

        byte[] bytes = zookeeper.getData("/zxw",false,null);
        System.out.println(new String(bytes));


        Stat stat = zookeeper.setData("/zxw","haha".getBytes() ,-1);


        zookeeper.create("/zxw/","seqhh".getBytes()
                ,ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT_SEQUENTIAL);

//        zookeeper.delete("/zxw" , stat.getVersion());

        zookeeper.close();
    }

    public static void connect() throws Exception
    {
        // 使用CountDownLatch，使主线程等待
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        zookeeper = new ZooKeeper(CONNECTSTRING, 10000, new Watcher()
        {
            @Override
            public void process(WatchedEvent event)
            {
                if(event.getState() == Event.KeeperState.SyncConnected)
                {
                    System.out.println("Watcher " + zookeeper.getState());
                    countDownLatch.countDown();
                }
            }
        });
        System.out.println("connect " + zookeeper.getState());
        countDownLatch.await();
        System.out.println("connect " + zookeeper.getState());
        // zookeeper.close();
    }



}
