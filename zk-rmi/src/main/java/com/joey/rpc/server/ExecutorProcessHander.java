package com.joey.rpc.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xiaowu.zhou@tongdun.cn on 2018/6/13.
 */
public class ExecutorProcessHander {

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static void doProcess(ProcessHandler processRunnale){

        executorService.submit(processRunnale);
    }



}
