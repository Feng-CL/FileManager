package com.scut.filemanager.ui.protocols;

//对话框结束回调
public interface DialogCallBack {
    void onDialogClose(boolean updateView);
    void onDialogCancel();
    void onDialogHide();
    void onDialogNeutralClicked();
}
