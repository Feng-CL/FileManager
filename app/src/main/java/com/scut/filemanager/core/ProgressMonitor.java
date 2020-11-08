package com.scut.filemanager.core;

public interface ProgressMonitor<K,V>{

    //progress' report callback

    public void onProgress(K key,V value);
    public void onFinished();
    public void onStart();
    public void onStop(PROGRESS_STATUS status);

    public void onSubProgress(int taskId,K key,V value);
    public void onSubTaskStart(int taskId);
    public void onSubTaskStop(int taskId,PROGRESS_STATUS status);
    public void onSubTaskFinish(int taskId);

    public void receiveMessage(String msg);
    public void receiveMessage(int code, String msg); //可以使用代码代表信息类型，具体代码要看使用该协议的双方对象


    //control signal receive function
    //periodically check the function, but causes busy waiting
    public boolean interruptSignal();
    public boolean abortSignal();


    enum PROGRESS_STATUS{
        GOING,
        PAUSED,
        FAILED,
        ABORTED
    }
}
