package com.joey.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/10.
 */
public class WatcherDemo {

    // 集群环境用,隔开
    private static final String CONNECTSTRING = "192.168.0.118:2181,192.168.0.119:2181,192.168.0.120:2181";
    private static ZooKeeper zookeeper;

    public static void main(String[] args) throws Exception
    {
        connect();

        Stat stat = zookeeper.exists("/zxw",true);


        System.out.println(stat);

        if (stat != null){
            zookeeper.getData("/zxw" , true ,stat);

            zookeeper.getChildren("/zxw" , true);
        }

        System.in.read();

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
                    System.out.println("watch connect state : " + zookeeper.getState());
                    countDownLatch.countDown();
                }

                System.out.println("watcher event type : " + event.getType());
            }
        });
        System.out.println("connect " + zookeeper.getState());
        countDownLatch.await();
        System.out.println("connect " + zookeeper.getState());
        // zookeeper.close();
    }

    /**
     * 删除节点
     */
    public static void deleteNode(String nodePath, int version) throws Exception
    {
        zookeeper.delete(nodePath, version);
        System.out.println("删除成功");
    }

    /**
     * 修改节点
     */
    public static void setNode(String nodePath, String value, int version) throws Exception
    {
        Stat stat = zookeeper.setData(nodePath, value.getBytes(), version);
        System.out.println("修改成功 " + stat);
    }

    /**
     * 创建节点
     */
    public static void createNode(String nodePath, String value) throws Exception
    {
        String result = zookeeper.create(nodePath, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        System.out.println("创建成功 " + result);
    }

    /**
     * 创建连接
     */



}
