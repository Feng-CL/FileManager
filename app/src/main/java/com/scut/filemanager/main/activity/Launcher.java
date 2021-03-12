package com.scut.filemanager.main.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.scut.filemanager.FileManager;
import com.scut.filemanager.R;
import com.scut.filemanager.main.MainActivity;

import java.util.Timer;
import java.util.TimerTask;

public class Launcher extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*

		if (VERSION.SDK_INT > 11 && VERSION.SDK_INT < 19) { // lower api
			View v = getWindow().getDecorView();
			v.setSystemUiVisibility(View.GONE);
		} else if (Build.VERSION.SDK_INT >= 19) {
			// for new api versions.
			View decorView = getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
					| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
					| View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			decorView.setSystemUiVisibility(uiOptions);
         */
        setContentView(R.layout.activity_launcher);

        Timer timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent  startMainActivityIntent=new Intent(getApplicationContext(), MainActivity.class);
                //startMainActivityIntent.addFlags(Intent.)
                startActivity(startMainActivityIntent);
                finish();

            }
        }, FileManager.START_MAIN_DELAY);
    }


}
