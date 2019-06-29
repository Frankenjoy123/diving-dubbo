package com.joey.rpc.server;

import com.joey.rpc.ISportService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
public class ServerDemo {

    public static void main(String[] args) {


        ISportService sportService = new SportServiceImpl();

        RpcServer rpcServer = new RpcServer();
        rpcServer.publish(sportService , 8888);

    }
}
