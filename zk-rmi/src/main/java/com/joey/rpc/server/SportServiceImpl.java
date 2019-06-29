package com.joey.rpc.server;

import com.joey.rpc.ISportService;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
public class SportServiceImpl implements ISportService {

    @Override
    public String doSport(String msg) {
        return "do sport with " + msg ;
    }
}
