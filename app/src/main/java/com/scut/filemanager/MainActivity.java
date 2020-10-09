package com.scut.filemanager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import com.scut.filemanager.*;

public class MainActivity extends AppCompatActivity
{
    MainController controller=null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        TextView tv=(TextView)findViewById(R.id.textview3);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        controller=new MainController();
        controller.startService(this);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_frame_menu,menu);
        return true;
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

    @Override
    public boolean onKeyDown(int keycode,KeyEvent k_ev){
        if(keycode==KeyEvent.KEYCODE_BACK){
            try {
                return controller.handleKeyDownEvent_callback();
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
        }
        else{
            return true; //按键事件在该层被消费
        }
    }
}