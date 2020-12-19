package com.scut.filemanager.core.internal;

import com.scut.filemanager.core.ProgressMonitor;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Future;

/*
@Description: 该抽象类实现了ProgressMonitor 的大多数方法，并提供了必要的被动式的信号机制。
该类方便了要利用ProgressMonitor接口的子类的按需扩展。
 */
abstract public class AbstractTaskMonitor<K,V> implements ProgressMonitor<K,V>{
    protected ProgressMonitor.PROGRESS_STATUS progress_status;
    protected boolean cancelSignal;
    protected boolean interruptSignal;
    protected Stack<Map.Entry<Integer,String>> MessagesStack;


    public ProgressMonitor.PROGRESS_STATUS getProgressStatus(){
        return progress_status;
    }

    public abstract void sendCancelSignal(int slot);
    protected abstract void pushMessage(int code, String msg);

    @Override
    public void onProgress(K key, V value) {

    }

    @Override
    public void onFinished() {
        progress_status=PROGRESS_STATUS.COMPLETED;
    }

    @Override
    public void onStart() {
        progress_status=PROGRESS_STATUS.GOING;
    }

    @Override
    public void onStart(Future<?> future) {

    }

    @Override
    public void onStop(PROGRESS_STATUS status) {
        progress_status=status;
    }

    @Override
    public void onSubProgress(int taskId, K key, V value) {

    }

    @Override
    public void onSubTaskStart(int taskId) {

    }

    @Override
    public void onSubTaskStop(int taskId, PROGRESS_STATUS status) {

    }

    @Override
    public void onSubTaskFinish(int taskId) {

    }

    @Override
    public void receiveMessage(String msg) {

    }

    @Override
    public void receiveMessage(int code, String msg) {

    }

    @Override
    public void describeTask(int taskId, String title) {

    }

    @Override
    public boolean interruptSignal() {
        return interruptSignal;
    }

    @Override
    public boolean abortSignal() {
        return cancelSignal;
    }

    @Override
    public boolean abortSignal(int slot) {
        return false;
    }
}
