package com.scut.filemanager.core.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//管理线程服务类，暂时为做优化
public class AppThreadPoolProxy {


    static public ExecutorService getAnNetExecutorService(){
        ExecutorService executorService= Executors.newSingleThreadExecutor();
        return executorService;
    }

}
