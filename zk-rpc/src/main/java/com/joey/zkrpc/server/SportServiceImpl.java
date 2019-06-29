package com.joey.zkrpc.server;


import com.joey.zkrpc.ISportService;
import com.joey.zkrpc.server.anno.RpcAnnotation;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
@RpcAnnotation(value = ISportService.class)
public class SportServiceImpl implements ISportService {

    @Override
    public String doSport(String msg) {
        return "do sport with " + msg ;
    }
}
