package com.scut.filemanager.ui.protocols;

public interface ProgressDialogContentProvider extends DialogCallBack {


    public int getMaxMeasure(); //获取进度总评估
    public boolean isPause();
}
