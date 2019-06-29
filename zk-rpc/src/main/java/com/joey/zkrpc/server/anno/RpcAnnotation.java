package com.joey.zkrpc.server.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/20.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcAnnotation {

    Class<?> value();

    String version() default "";
}
