package com.joey.rpc.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/19.
 */
public class RpcServer {


    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public void publish(Object service , int port){

        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (true){

                Socket socket = serverSocket.accept();

                executorService.submit(new ProcessHandler(socket , service));

            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
