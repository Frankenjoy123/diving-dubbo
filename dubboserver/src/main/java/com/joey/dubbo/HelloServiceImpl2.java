package com.joey.dubbo;

import com.joey.hello.IhelloService;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/14.
 */
public class HelloServiceImpl2 implements IhelloService{
    @Override
    public String sayHello(String msg) {
        return "hello, I am server 2" +msg;
    }
}
