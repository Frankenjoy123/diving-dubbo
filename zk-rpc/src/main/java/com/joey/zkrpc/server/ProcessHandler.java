package com.joey.zkrpc.server;

import com.joey.zkrpc.RpcRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
public class ProcessHandler implements Runnable{

    private Socket socket;
    private Map<String , Object> handleMap;


    public ProcessHandler(Socket socket, Map<String, Object> handleMap) {
        this.socket = socket;
        this.handleMap = handleMap;
    }

    @Override
    public void run() {


        ObjectInputStream inputStream = null;
        ObjectOutputStream outputStream = null;
        try {

            inputStream = new ObjectInputStream(socket.getInputStream());
            RpcRequest request = (RpcRequest) inputStream.readObject();

            Object result = invoke(request);

            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(result);
            outputStream.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (inputStream!=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private Object invoke(RpcRequest request) {

        //通过反射调用
        Class<?>[] paramClassArr = new Class[request.getParams().length];

        for (int i=0 ; i<request.getParams().length ; i++){

            paramClassArr[i] = request.getParams()[i].getClass();

        }

        String serviceName = request.getClassName();
        String version = request.getVersion();
        if (version!=null && !version.equals("")){
            serviceName = serviceName +'-' +version;
        }

        //从handleMap中，根据可端端的地址，去拿到响应的服务，通过反射发起调用
        Object service = handleMap.get(serviceName);

        try {
            Method method = service.getClass().getMethod(request.getMethodName(), paramClassArr);

            return method.invoke(service , request.getParams());

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }
}
