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


    //permission
    private static String[] PERMISSION_STORAGE={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static int REQUEST_PERMISSION_CODE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }







    String [] data=new String[]{
        "item1","item2","item3","item4","item5"
    };

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        TextView tv=(TextView)findViewById(R.id.textview3);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RatingBar sb= findViewById(R.id.rb);
        ArrayAdapter<String> aaData=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,data);
        TextView tv=findViewById(R.id.textView);

        File ExternalRoot=android.os.Environment.getExternalStorageDirectory();
        File DataDir=android.os.Environment.getDataDirectory();
        com.scut.filemanager.core.Service service=com.scut.filemanager.core.Service.getInstance(this);
        tv.setText("internalPirvateDir: "+service.getInternalPrivateDirectoryPathName()
        +"\nexternalPrivateDir:"+service.getExternalPrivateDirectoryPathName()+
                "\nExternalPublicRootDir: "+service.getRootDirPathName());
        File[] files=getExternalFilesDirs(null);
        StringBuilder sdcard_dir=new StringBuilder(files[1].getAbsolutePath());
        int cut_pos=sdcard_dir.indexOf("/Android");
        sdcard_dir.delete(cut_pos,sdcard_dir.length());
        File sdcard_file=new File(sdcard_dir.toString());
    }



}