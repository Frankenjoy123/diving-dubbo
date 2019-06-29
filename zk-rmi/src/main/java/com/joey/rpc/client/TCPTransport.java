package com.joey.rpc.client;

import com.joey.rpc.RpcRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/20.
 */
public class TCPTransport {

    private String host;

    private int port;

    public TCPTransport(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private Socket newSocket() throws IOException {

        Socket socket = new Socket(host,port);
        return socket;
    }


    public Object send(RpcRequest request){

        ObjectOutputStream obStream = null;

        ObjectInputStream objInStream = null;

        try {
            Socket socket = newSocket();
            obStream =  new ObjectOutputStream(socket.getOutputStream());

            obStream.writeObject(request);
            obStream.flush();

            objInStream = new ObjectInputStream(socket.getInputStream());
            return  objInStream.readObject();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            if (obStream != null){
                try {
                    objInStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (objInStream != null){
                try {
                    objInStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}
