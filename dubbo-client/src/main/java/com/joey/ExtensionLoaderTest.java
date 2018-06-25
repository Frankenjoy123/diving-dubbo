package com.joey;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Protocol;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/24.
 */
public class ExtensionLoaderTest {

    public static void main(String[] args) {

        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
                .getDefaultExtension();

        System.out.println(protocol.getDefaultPort());
    }
}
