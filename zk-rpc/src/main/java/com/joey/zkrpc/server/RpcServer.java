package com.joey.zkrpc.server;

import com.joey.zkrpc.ISportService;
import com.joey.zkrpc.server.anno.RpcAnnotation;
import com.joey.zkrpc.server.zk.IRegisterCenter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/19.
 */
public class RpcServer {


    private static ExecutorService executorService = Executors.newCachedThreadPool();

    private IRegisterCenter registerCenter;

    private String address;

    private Map<String , Object>  handleMap = new HashMap<>();

    public RpcServer(IRegisterCenter registerCenter, String address) {
        this.registerCenter = registerCenter;
        this.address = address;
    }

    public void bind(Object... services){

        for (Object service : services){
            RpcAnnotation rpcAnnotation = service.getClass().getAnnotation(RpcAnnotation.class);
            String serviceName = rpcAnnotation.value().getName();

            String version = rpcAnnotation.version();

            if (version != null && !version.equals("")){

                serviceName = serviceName + "-" + version;
            }

            handleMap.put(serviceName , service);
        }

    }

    public void publish(){


        for (String key : handleMap.keySet()){
            registerCenter.register(key, address);
            System.out.println("注册服务成功: " + key + " -> " + address);
        }


        int port = Integer.parseInt(address.split(":")[1]);

        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (true){

                Socket socket = serverSocket.accept();

                executorService.submit(new ProcessHandler(socket , handleMap));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
