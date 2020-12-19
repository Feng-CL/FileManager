package com.scut.filemanager.core.concurrent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;


/*
a simple thread pool
 */
public class SharedThreadPool {
    //three level pool
    static ExecutorService executor1,executor2,executor3;
    static SharedThreadPool sharedThreadPool;

    private SharedThreadPool(){
        executor1= Executors.newFixedThreadPool(3);
        executor2=Executors.newFixedThreadPool(2);
        executor3=Executors.newSingleThreadExecutor();
    }

    static public SharedThreadPool getInstance(){
        if(sharedThreadPool==null){
            sharedThreadPool=new SharedThreadPool();
        }
        return sharedThreadPool;
    }

    public enum PRIORITY{
        HIGH,MEDIUM,LOW
    }

    /*
    @Description: 指定任务和等级，让共享线程池完成此任务，除非任务指定了监控器，即
    具有自主的进度控制方法，否则这将导致任务无法被跟踪。
     */
    public void executeTask(Runnable task,PRIORITY level){
        Future future;
        switch (level){
            case LOW:
                executor3.execute(task);
                break;
            case MEDIUM:
                executor2.execute(task);
                break;
            case HIGH:
                executor1.execute(task);
                break;
            default:
                break;
        }
    }

    /*
    @Description: 提交一个可运行的任务执行，并返回一个表示该任务的未来。 未来的get方法将返回null 成功完成时。
     */
    public  Future<?> submit(Runnable task, PRIORITY level){
        Future<?> future;
        switch (level){
            case LOW:
                future=executor3.submit(task);
                break;
            case MEDIUM:
                future=executor2.submit(task);
                break;
            case HIGH:
                future=executor1.submit(task);
                break;
            default:
                future=null;
                break;

        }
        return future;
    }

    public <V> void executeTask(RunnableFuture<V> task,PRIORITY level){
        switch (level){
            case LOW:
                executor3.execute(task);
                break;
            case MEDIUM:
                executor2.execute(task);
                break;
            case HIGH:
                executor1.execute(task);
                break;
            default:
                break;
        }
    }

    /*
    @Description: 启动有序关闭，其中先前提交的任务将被执行，但不会接受任何新任务。
     */

    public void shutdown(PRIORITY level){
        switch(level){
            case LOW:
                executor3.shutdown();
                break;
            case MEDIUM:
                executor2.shutdown();
                break;
            case HIGH:
                executor1.shutdown();
                break;
            default:break;
        }
    }

    /*
    @Description: 尝试停止所有主动执行的任务，停止等待任务的处理，并返回正在等待执行的任务列表。
     */
    public List<Runnable> shutdownNow(PRIORITY level){
        List<Runnable> runnableList;
        switch(level){
            case LOW:
                runnableList= executor3.shutdownNow();
                break;
            case MEDIUM:
                runnableList=executor2.shutdownNow();
                break;
            case HIGH:
                runnableList=executor1.shutdownNow();
                break;
            default:
                runnableList=null;
                break;
        }
        return runnableList;
    }
}
