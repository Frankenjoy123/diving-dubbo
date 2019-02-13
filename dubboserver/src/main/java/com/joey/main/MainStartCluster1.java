package com.joey.main;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/14.
 */
public class MainStartCluster1 {

    public static void main(String[] args) throws IOException {

        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("classpath:/dubbo-server-1.xml");

        context.start();

        System.in.read();
    }
}
