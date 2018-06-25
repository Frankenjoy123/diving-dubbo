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

        ExtensionFactory extensionFactory = loader.getAdaptiveExtension();


//        extensionFactory.getExtension()

        System.out.println(extensionFactory);
    }

    @Test
    public void testProtocolLoader(){

        ExtensionLoader<Protocol> loader =
                ExtensionLoader.getExtensionLoader(Protocol.class);

        System.out.println(loader.getAdaptiveExtension());

//        System.out.println(protocol.getDefaultPort());

    }


}
