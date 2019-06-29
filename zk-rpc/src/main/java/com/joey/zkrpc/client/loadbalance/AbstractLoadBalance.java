package com.joey.zkrpc.client.loadbalance;

import java.util.List;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/21.
 */
public abstract class AbstractLoadBalance {


    public String loadSelect(List<String> addressList){

        if (addressList == null || addressList.size()==0){
            return null;
        }

        if (addressList.size()==1){
            return addressList.get(0);
        }

        return  doSelect(addressList);


    }


    protected abstract String doSelect(List<String> addressList);


}
