package com.scut.filemanager.ui.controller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewStub;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.scut.filemanager.R;
import com.scut.filemanager.core.Service;

/*
用于控制操作栏的一个类
目前仅用于加载必要的操作栏
 */
public class OperationBarController extends BaseController {

    private LinearLayout linearLayout;
    private int layout_resource_id = R.layout.operation_bar;

    private View btn_open;
    private View btn_copy;
    private View btn_move;
    private View btn_rename;
    private View btn_delete;
    private View btn_more;
    private View btn_cancel;
    private View btn_paste;
    private View btn_newFolder;


    private final int open_button_id = R.id.operation_button_open;  //1
    private final int copy_button_id = R.id.operation_button_copy;    //2
    private final int move_button_id = R.id.operation_button_move;    //3
    private final int rename_button_id = R.id.operation_button_rename; //4
    private final int delete_button_id = R.id.operation_button_delete; //5
    private final int more_button_id = R.id.operation_button_more; //6
    private final int cancel_button_id = R.id.operation_button_cancel; //7
    private final int paste_button_id = R.id.operation_button_paste; //8
    private final int newFolder_button_id = R.id.operation_button_newFolder; //9


    private static int fadeInFadeOut_durationTime=100;




    private static class MessageCode{
        static private final int SINGLE_SELECTED = 1;
        static private final int MULTI_SELECTED = 2;
        static private final int SELECT_CANCELED = 3;
        static private final int COPY_MOVE_STATUS = 4;
        static private final int STATIC_STATUS = 5;
    }

    /*
    @Description: 控制操作栏初始化需要载入图标，并接受父控制器的状态
    同时，设置好状态刷新的回调函数。
     */
    public OperationBarController(ViewStub stub){
        super();
        stub.setLayoutResource(layout_resource_id);
        linearLayout=(LinearLayout)stub.inflate();
        stub.setVisibility(View.VISIBLE);

        loadOperationWidget();
        //linearLayout.setVisibility(View.INVISIBLE);
        this.mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case MessageCode.SINGLE_SELECTED:
                        singleSelectStatus();
                        break;
                    case MessageCode.MULTI_SELECTED:
                        multiSelectStatus();
                        break;
                    case MessageCode.SELECT_CANCELED:
                        disableButton();
                        break;
                    case MessageCode.COPY_MOVE_STATUS:
                        copyOrMoveStatus();
                        break;
                    case MessageCode.STATIC_STATUS:
                        setAllWidgetsGone();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    /*
    处理操作状态的回调
     */
    public void onOperationStatusChange(TabDirectoryViewController.OPERATION_STATE operation_state, int countCheckBox){
        switch (operation_state){
            case SELECTING:
                if(countCheckBox==1)
                    mHandler.sendEmptyMessage(MessageCode.SINGLE_SELECTED);
                else if(countCheckBox>1)
                    mHandler.sendEmptyMessage(MessageCode.MULTI_SELECTED);
                else if(countCheckBox==0)
                    mHandler.sendEmptyMessage(MessageCode.SELECT_CANCELED);
                break;
            case STATIC:
                mHandler.sendEmptyMessage(MessageCode.STATIC_STATUS);
                break;
            case COPY:
                mHandler.sendEmptyMessage(MessageCode.COPY_MOVE_STATUS);
                break;
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    private void loadOperationWidget(){
        btn_open = linearLayout.findViewById(open_button_id);
        btn_copy = linearLayout.findViewById(copy_button_id);
        btn_move = linearLayout.findViewById(move_button_id);
        btn_rename = linearLayout.findViewById(rename_button_id);
        btn_delete = linearLayout.findViewById(delete_button_id);
        btn_more = linearLayout.findViewById(more_button_id);
        btn_cancel = linearLayout.findViewById(cancel_button_id);
        btn_paste = linearLayout.findViewById(paste_button_id);
        btn_newFolder = linearLayout.findViewById(newFolder_button_id);

        btn_open.setTag(1);
        btn_copy.setTag(2);
        btn_move.setTag(3);
        btn_rename.setTag(4);
        btn_delete.setTag(5);
        btn_more.setTag(6);
        btn_cancel.setTag(7);
        btn_paste.setTag(8);
        btn_newFolder.setTag(9);
        //初始全部设置为GONE
        setAllWidgetsGone();
    }



    public void setButtonOnClickListener(View.OnClickListener onClickListener){
        btn_open.setOnClickListener(onClickListener);
        btn_copy.setOnClickListener(onClickListener);
        btn_move.setOnClickListener(onClickListener);
        btn_rename.setOnClickListener(onClickListener);
        btn_delete.setOnClickListener(onClickListener);
        btn_more.setOnClickListener(onClickListener);
        btn_cancel.setOnClickListener(onClickListener);
        btn_paste.setOnClickListener(onClickListener);
        btn_newFolder.setOnClickListener(onClickListener);
    }


    public void onScrollStateChange(TabDirectoryViewController.OPERATION_STATE state){
        if(state == TabDirectoryViewController.OPERATION_STATE.SCROLLING){
            //hide
            fadeOut();
        }
        else {
            fadeIn();
        }
    }


    private void fadeOut(){
        linearLayout.setAlpha(1f);
        linearLayout.animate().alpha(0f)
                .setDuration(OperationBarController.fadeInFadeOut_durationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        linearLayout.setVisibility(View.GONE);
                    }
                });
    }

    private void fadeIn(){
        linearLayout.setAlpha(0f);
        linearLayout.setVisibility(View.VISIBLE);
        linearLayout.animate().alpha(1f)
                .setDuration(OperationBarController.fadeInFadeOut_durationTime)
                .setListener(null);
    }

    private void singleSelectStatus(){
        setAllWidgetsGone();
        for(int i = 0; i < 5; i++){
            linearLayout.getChildAt(i).setVisibility(View.VISIBLE);
        }
        btn_more.setVisibility(View.VISIBLE);
    }

    private void multiSelectStatus(){
        setAllWidgetsGone();
        btn_copy.setVisibility(View.VISIBLE);
        btn_move.setVisibility(View.VISIBLE);
        btn_delete.setVisibility(View.VISIBLE);
        btn_more.setVisibility(View.VISIBLE);

    }

    private void copyOrMoveStatus(){
        setAllWidgetsGone();
        for(int i=5; i<linearLayout.getChildCount();i++){
            linearLayout.getChildAt(i).setVisibility(View.VISIBLE);
        }
    }

    private void setAllWidgetsGone(){
        for(int i = 0; i<linearLayout.getChildCount(); i++){
            linearLayout.getChildAt(i).setVisibility(View.GONE);
        }
    }

    private void disableButton(){
        setAllWidgetsGone();
    }

    @Override
    public Context getContext() {
        return parentController.getContext();
    }

    @Override
    public Handler getHandler() {
        return null;
    }

    @Override
    public Service getFileManagerCoreService() {
        return this.parentController.getFileManagerCoreService();
    }
}
