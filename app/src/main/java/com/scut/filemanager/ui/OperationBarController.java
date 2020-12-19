package com.scut.filemanager.ui;

import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.scut.filemanager.R;

/*
用于控制操作栏的一个类
目前仅用于加载必要的操作栏
 */
public class OperationBarController {

    private LinearLayout linearLayout;
    private int layout_resource_id= R.layout.operation_bar;

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
        icon.setImageResource(R.drawable.icon_basic_function_copy);
        textView.setText("copy");
    }

    /*
    处理操作状态的回调
     */
    public void onOperationStatusChange(){}
}
