package com.joey.rpc;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = -5216510092895563469L;


    private String className;

    private String methodName;

    private Object[] params;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }
}




