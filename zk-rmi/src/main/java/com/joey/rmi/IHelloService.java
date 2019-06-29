package com.joey.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/11.
 */
public interface IHelloService extends Remote{
    String sayHello(String msg) throws RemoteException;
}
