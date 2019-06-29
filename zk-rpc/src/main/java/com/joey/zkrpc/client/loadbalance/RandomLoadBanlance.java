package com.joey.zkrpc.client.loadbalance;

import java.util.List;
import java.util.Random;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/21.
 */
public class RandomLoadBanlance extends AbstractLoadBalance{

    @Override
    protected String doSelect(List<String> addressList) {

        Random random = new Random();

        int i = random.nextInt(addressList.size());

        return addressList.get(i);
    }
}
