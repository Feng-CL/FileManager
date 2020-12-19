package com.scut.filemanager.core;

import java.util.concurrent.Future;


/*
@Description: 被动式监听协议
虽然是准备了多个接口，但实际是使用到的却比较少，斟酌选择
 */
public interface ProgressMonitor<K,V>{

    //progress' report callback

    public void onProgress(K key,V value);
    public void onFinished();
    public void onStart();  //一般在此方法调用后设置PROGRESS_STATUS为GOING
    public void onStart(Future<?> future); //新增接口，onStart()开始的时候获取到整个任务的future，使用该方法可以通过future强制终止该任务
    public void onStop(PROGRESS_STATUS status);

    public void onSubProgress(int taskId,K key,V value);
    public void onSubTaskStart(int taskId);
    public void onSubTaskStop(int taskId,PROGRESS_STATUS status);
    public void onSubTaskFinish(int taskId);

    public void receiveMessage(String msg);
    public void receiveMessage(int code, String msg); //可以使用代码代表信息类型，具体代码要看使用该协议的双方对象
    public void describeTask(int taskId,String title);//描述任务

    //control signal receive function

    /*
       @Description: 基于忙等的一种中断方式，允许哪些任务可以被中断，需要参考对应任务的
       监视器协议
       periodically check the function, but causes busy waiting
     */
    public boolean interruptSignal();

    /*
    @Description: 所有任务均可通过设置 丢弃信号位 来判断任务是否应该停止并结束
     */
    public boolean abortSignal();
    public boolean abortSignal(int slot);


    //suggestive method for this
    //public boolean isDone();
    //public boolean isStop();
    //public void join();

    public enum PROGRESS_STATUS{
        GOING,
        PAUSED,
        FAILED,
        ABORTED,
        COMPLETED, //set to it when onFinished() is called
    }


}
