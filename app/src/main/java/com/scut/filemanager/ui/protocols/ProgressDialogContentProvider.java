package com.scut.filemanager.ui.protocols;

public interface ProgressDialogContentProvider extends DialogCallBack {
    public String getSpeedDescription();// 获取速度描述，具体取决于内容提供类
    public int getMaxMeasure(); //获取进度总评估
    public int getProgress(); //获取此时的进度
    public int getProgressIncrement(); //获取进度在某一时刻相对上一时刻取得的值的增量，相当于getProgress()-last.getProgress()
}
