package com.scut.filemanager.core.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AppThreadPoolProxy {


    static public ExecutorService getAnNetExecutorService(){
        ExecutorService executorService= Executors.newSingleThreadExecutor();
        return executorService;
    }

}
