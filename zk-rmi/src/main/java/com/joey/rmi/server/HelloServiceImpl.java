package com.joey.rmi.server;

import com.joey.rmi.IHelloService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/11.
 */
public class HelloServiceImpl extends UnicastRemoteObject implements IHelloService{

    protected HelloServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public String sayHello(String msg) {
        return "hello,"+msg;
    }


}
