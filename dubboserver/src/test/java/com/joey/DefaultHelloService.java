package com.joey;

import com.joey.hello.IhelloService;
import sun.jvm.hotspot.HelloWorld;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2019/2/13.
 */
public class DefaultHelloService implements IhelloService {

    private IhelloService ihelloService;

    public DefaultHelloService(IhelloService ihelloService) {
        this.ihelloService = ihelloService;
    }

    @Override
    public String sayHello(String msg) {
        return ihelloService.sayHello(msg);
    }
}
