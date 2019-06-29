package com.joey.zkrpc.test;

import com.joey.zkrpc.ISportService;
import com.joey.zkrpc.server.SportServiceImpl;
import com.joey.zkrpc.server.anno.RpcAnnotation;
import org.junit.Test;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/20.
 */
public class AnnoTest {

    @Test
    public void annoTest(){

        ISportService sportService = new SportServiceImpl();

        Class clazz = sportService.getClass();

        RpcAnnotation anno = (RpcAnnotation) clazz.getAnnotation(RpcAnnotation.class);


        System.out.println(anno);

    }
}
