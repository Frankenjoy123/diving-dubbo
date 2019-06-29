package com.joey.zkrpc.server;


import com.joey.zkrpc.ISportService;
import com.joey.zkrpc.server.zk.IRegisterCenter;
import com.joey.zkrpc.server.zk.RegisterCenterImpl;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
public class ServerDemo {

    public static void main(String[] args) {


        ISportService sportService = new SportServiceImpl();

        IRegisterCenter registerCenter = new RegisterCenterImpl();

        RpcServer rpcServer = new RpcServer(registerCenter , "127.0.0.1:8888");
        rpcServer.bind(sportService);
        rpcServer.publish();

    }
}
