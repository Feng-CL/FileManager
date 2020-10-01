package com.scut.filemanager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.List;


public class MainActivity extends AppCompatActivity

{

    String [] data=new String[]{
        "item1","item2","item3","item4","item5"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        TextView tv=(TextView)findViewById(R.id.textview3);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RatingBar sb= findViewById(R.id.rb);
        ArrayAdapter<String> aaData=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,data);
        ((ListView)findViewById(R.id.listview)).setAdapter(aaData);
    }



}