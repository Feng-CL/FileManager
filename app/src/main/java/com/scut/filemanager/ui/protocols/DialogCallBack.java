package com.scut.filemanager.ui.protocols;

import android.content.DialogInterface;

/**
 * 对话框的通用回调函数，响应的对话框代理类的实现可以不完全使用
 * 但必须说明清楚使用了哪些
 */
public interface DialogCallBack {
    /**
     * 对话框的最后一个回调，调用此函数后，对话框将关闭。
     * @param updateView true表示需要更新对话框关闭后的下一个视图
     */
    void onDialogClose(DialogInterface dialog,boolean updateView);

    /**
     * 对话框取消后的回调
     */
    void onDialogCancel(DialogInterface dialog);

    /**
     * 对话框隐藏后的第一个回调
     */
    void onDialogHide(DialogInterface dialog);

    /**
     * 对话框中间按钮按下后的回调
     */
    void onDialogNeutralClicked(DialogInterface dialog);

    /**
     * 对话框确认键按下后的回调
     */
    void onDialogOk(DialogInterface dialog);
}
