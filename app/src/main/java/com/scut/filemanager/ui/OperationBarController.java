package com.scut.filemanager.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.scut.filemanager.FMGlobal;
import com.scut.filemanager.R;

/*
用于控制操作栏的一个类
目前仅用于加载必要的操作栏
 */
public class OperationBarController extends BaseController{

    private LinearLayout linearLayout;
    private int layout_resource_id= R.layout.operation_bar;

    private static int fadeInFadeOut_durationTime=100;
    /*
    @Description: 控制操作栏初始化需要载入图标，并接受父控制器的状态
    同时，设置好状态刷新的回调函数。
     */
    public OperationBarController(ViewStub stub){
        stub.setLayoutResource(layout_resource_id);
        linearLayout=(LinearLayout)stub.inflate();
        stub.setVisibility(View.VISIBLE);
        loadOperationWidget();

    }

    private void loadOperationWidget(){
        ImageView icon=linearLayout.findViewById(R.id.operation_icon);
        TextView  textView=linearLayout.findViewById(R.id.operation_name);
        icon.setImageResource(R.drawable.icon_function_copy);
        textView.setText("copy");
    }

    /*
    处理操作状态的回调
     */
    public void onOperationStatusChange(){}

    public void onScrollStateChange(TabViewController.OPERATION_STATE state){
        if(state== TabViewController.OPERATION_STATE.SCROLLING){
            //hide
            fadeOut();
            //Log.i("OperationBar", "fade out");
        }
        else{
            fadeIn();
            //Log.i("OperationBar","fade in");
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

    @Override
    public Context getContext() {
        return parentController.getContext();
    }

    @Override
    public Handler getHandler() {
        return null;
    }


    //zc code
    private void oneFileSelected(){
        ImageView icon=linearLayout.findViewById(R.id.operation_icon);
        TextView textView=linearLayout.findViewById(R.id.operation_name);
       // icon.setImageResource(R.drawable.icon_basic_function_copy);
        textView.setText("oneFile");
    }

    private void moreThanOneFileSelected(){
        ImageView icon=linearLayout.findViewById(R.id.operation_icon);
        TextView  textView=linearLayout.findViewById(R.id.operation_name);
      //  icon.setImageResource(R.drawable.icon_basic_function_copy);
        textView.setText("moreFile");
    }

    private void disableButton(){
        ImageView icon=linearLayout.findViewById(R.id.operation_icon);
        TextView  textView=linearLayout.findViewById(R.id.operation_name);
     //   icon.setImageResource(R.drawable.icon_basic_function_copy);
        textView.setText("disable");
    }

    public void updateOperationWidget(TabViewController.OPERATION_STATE operation_state,int countCheckBox){
        switch (operation_state){
            case SELECTING:
                if(countCheckBox==1)
                    oneFileSelected();
                else if(countCheckBox>1)
                    moreThanOneFileSelected();
                else if(countCheckBox==0)
                    disableButton();
                break;
            case STATIC:
                loadOperationWidget();
        }

    }

    //....................
}
