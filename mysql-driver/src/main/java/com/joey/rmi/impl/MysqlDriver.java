package com.joey.rmi.impl;

import com.joey.spi.IDatabaseDriver;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/23.
 */
public class MysqlDriver implements IDatabaseDriver{
    @Override
    public String connet(String host) {
        return "mysql: " + host;
    }
}
