package com.joey;

import com.alibaba.dubbo.common.extension.ExtensionFactory;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Protocol;
import org.junit.Test;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/25.
 */
public class ExtensionLoadTest {

    @Test
    public void testExtensionFactoryLoader(){

        ExtensionLoader<ExtensionFactory> loader =
                ExtensionLoader.getExtensionLoader(ExtensionFactory.class);

        //返回AdaptiveExtensionFactory
        ExtensionFactory extensionFactory = loader.getAdaptiveExtension();

        System.out.println(extensionFactory);
    }

    @Test
    public void testProtocolLoader(){

        ExtensionLoader<Protocol> loader =
                ExtensionLoader.getExtensionLoader(Protocol.class);

        //返回Protocol$Adaptive的实例
        Protocol protocol = loader.getAdaptiveExtension();

        System.out.println(protocol);

    }

    @Test
    public void testWrapperDubboProtocol(){
        ExtensionLoader<Protocol> loader =
                ExtensionLoader.getExtensionLoader(Protocol.class);

        //返回listnerWrapper(filterWapper(dubboProtocol))的包装
        Protocol protocol = loader.getExtension("dubbo");

        System.out.println(protocol);
    }


}
