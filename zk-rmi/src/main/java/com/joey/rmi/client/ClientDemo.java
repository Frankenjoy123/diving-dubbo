package com.joey.rmi.client;

import com.joey.rmi.IHelloService;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/11.
 */
public class ClientDemo {

    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {

        IHelloService helloService = (IHelloService) Naming.lookup("rmi://127.0.0.1/Hello");

        String result = helloService.sayHello("joey_xiaowu");

        System.out.println(result);

    }
}
