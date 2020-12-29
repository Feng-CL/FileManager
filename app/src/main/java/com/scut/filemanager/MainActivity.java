package com.scut.filemanager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.util.List;

import com.scut.filemanager.*;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.concurrent.SharedThreadPool;
import com.scut.filemanager.ui.dialog.SingleLineInputDialogDelegate;

public class MainActivity extends AppCompatActivity

{
    MainController controller=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        TextView tv=(TextView)findViewById(R.id.textview3);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar)findViewById(R.id.my_toolbar));

        controller=new MainController();
        controller.startService(this);
        try {
            controller.startNetService();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            controller.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //启动前准备,暂时先不对闲置对象进行管理
        Log.d("deviceName:",android.os.Build.MODEL);

        FMGlobal.Default_shortAnimTime=getResources().getInteger(android.R.integer.config_shortAnimTime);
        FMGlobal.Default_longAnimTime=getResources().getInteger(android.R.integer.config_longAnimTime);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_frame_menu,menu);
        return true;
    }

    @Override
    protected void onDestroy() {

        //资源有序退出，或者保存一些数据
        //SharedThreadPool.getInstance().shutdownAll();
        super.onDestroy();
    }

    //permission
    private static String[] PERMISSION_STORAGE={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static int REQUEST_PERMISSION_CODE = 1;
    private static android.view.Menu _menu;

    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }


    //一个临时的onKeyDownListener MainActivity
    @Override
    public boolean onKeyDown(int keycode,KeyEvent k_ev){

        if(keycode==KeyEvent.KEYCODE_BACK){
            try {
                boolean comsume_result= controller.handleKeyDownEvent_callback();
                if(!comsume_result){
                    this.finish();
                }
                return true;

            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
        }
        else{
            super.onKeyDown(keycode,k_ev);
            return true; //按键事件在该层被消费
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        boolean consume;
        switch (item.getItemId()){
            case R.id.main_menu_item_sendbylan:
                invokeLanSenderActivity();
                consume=false;
                break;
            case R.id.main_menu_item_select_all:
                if(controller.getTabViewController()!=null){
                    controller.getTabViewController().setSelectAll();
                }
                consume=false;
                break;
            case R.id.main_menu_item_mkdir:
                if(controller.getTabViewController()!=null){

                }
                consume=false;
            default:
                consume=super.onOptionsItemSelected(item);
                break;
        }
        return consume;
    }

    private void invokeLanSenderActivity(){
        Intent intent=new Intent(this,LanSenderActivity.class);
        FMGlobal.netService=controller.netService;
        this.startActivity(intent);
    }
}