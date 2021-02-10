package com.scut.filemanager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.scut.filemanager.core.net.NetService;
import com.scut.filemanager.ui.adapter.DeviceListViewAdapter;
import com.scut.filemanager.ui.transaction.Request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/*
@Description: 该活动主要用于处理选择发送设备的，把选择的索引发送给原来的activity
 */
public class DeviceSelectActivity extends AppCompatActivity  {

    //UI outlets:
    Toolbar toolbar;
    ListView listView;

    //adapter for list view
    public DeviceListViewAdapter adapter;


    //NetService reference:
    NetService net_service;

    //state maintain
    //boolean isScanning=false;

    //ui refresh handler
    public Handler mHandler=new Handler(Looper.getMainLooper()){

        Timer refreshTimer=null;

        @Override
        public void handleMessage(@NonNull Message msg) {
           switch (msg.what){
               case UIMessageCode.UPDATE_TOOLBAR_SUBTITLE_TO_SCANNING:
                   if(refreshTimer==null){
                       refreshTimer=new Timer(true);
                   }
                        mTimerTask task=new mTimerTask();
                        refreshTimer.scheduleAtFixedRate(task,500,500);

                   break;
               case UIMessageCode.UPDATE_TOOLBAR_SUBTITLE_TO_FINISH:
                   if(refreshTimer!=null) {
                       refreshTimer.cancel();
                   }
                   toolbar.setSubtitle("finish scan");
                   break;
               case UIMessageCode.UNDATE_TOOLBAR_SUBTITLE_TEXT:
                   StringBuilder text= (StringBuilder) msg.obj;
                   toolbar.setSubtitle(text);
                   break;
               case UIMessageCode.NOTIFY_DATASET_CHANGE:
                   DeviceListViewAdapter.ItemData itemInMsg=(DeviceListViewAdapter.ItemData)msg.obj;

//                   Toast.makeText(DeviceSelectActivity.this,
//                           "pktId: "+String.valueOf(
//                                   itemInMsg.id
//                           ),
//                           Toast.LENGTH_SHORT).show();
                    if(adapter.contain(itemInMsg)){
                        adapter.addItems(itemInMsg);
                        adapter.notifyDataSetChanged();
                    }
                   break;
               case UIMessageCode.NOTIFY_CLOSE_ACTIVITY:
                   finish();
                   break;
               case UIMessageCode.UPDATE_TOOLBAR_SUBTITLE_TO_MESSAGE_CONTENT:
                   String subtitle= (String) msg.obj;
                   toolbar.setSubtitle(subtitle);
                   break;
               case UIMessageCode.UPDATE_TOOLBAR_NO_SUBTITLE:
                   toolbar.setSubtitle("");
                   break;
               case FMGlobal.MAKE_TOAST:
                   String tip= (String) msg.obj;
                   Toast.makeText(DeviceSelectActivity.this,tip,Toast.LENGTH_SHORT).show();
                   break;
               default:
                   super.handleMessage(msg);
                   break;
           }
        }

        StringBuilder displayText=new StringBuilder("scanning");
        class mTimerTask extends TimerTask {

            short state=0;
            @Override
            public void run() {
                state%=4;
                state++;
                Message msg=Message.obtain();
                msg.what=UIMessageCode.UNDATE_TOOLBAR_SUBTITLE_TEXT;
                if(state==3){
                    displayText.append('.');
                    //toolbar.setSubtitle(displayText);
                    msg.obj=displayText;
                    mHandler.sendMessage(msg);
                    displayText.setLength(8);
                    return;
                }
                displayText.append('.');
                msg.obj=displayText;
                mHandler.sendMessage(msg);
                //toolbar.setSubtitle(displayText);
            }
        }
    };



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(this.getClass().getName(),"lan sender activity start ");
        net_service=FMGlobal.netService;
        net_service.bindDeviceSelectActivity(this);
        setContentView(R.layout.activity_lan_sender);

        //retrieve UI outlets:
        this.listView=(ListView)findViewById(R.id.listview_for_displaying_devices);
        this.toolbar=(Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        //需要放在setSupportActionBar之后
        this.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(this.getClass().getName(),"back icon pressed, finishActivity ");
                mHandler.sendEmptyMessage(UIMessageCode.UPDATE_TOOLBAR_SUBTITLE_TO_FINISH);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask();
                }else{
                    finish();
                }
            }
        });

        adapter=new DeviceListViewAdapter(getLayoutInflater());
        this.listView.setAdapter(adapter);
       // setTitle("Scanning");
    }

    /*
        @Description:有点类似于ViewDidAppear
        @Params:null
    */

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_lan_sender,menu);
        return true;
    }

    public static class UIMessageCode{
        public static final int UPDATE_TOOLBAR_SUBTITLE_TO_SCANNING=0;
        public static final int UPDATE_TOOLBAR_SUBTITLE_TO_MESSAGE_CONTENT=6;
        public static final int UPDATE_TOOLBAR_SUBTITLE_TO_FINISH=1;
        public static final int UNDATE_TOOLBAR_SUBTITLE_TEXT =2;
        public static final int UPDATE_TOOLBAR_NO_SUBTITLE=7;

        public static final int NOTIFY_DATASET_CHANGE=3;
        public static final int NOTIFY_CLOSE_ACTIVITY=4;

        public static final int UPDATE_ERR_MSG=5;
    }

    public void setNetServiceRef(NetService ref){
        this.net_service=ref;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        boolean consume_result;
        switch (item.getItemId()){
            case R.id.menu_item_lan_scan:

                    //这里的scan按钮调用的函数需要修改一下，扫描是时刻进行的, 根据需要过滤各类包
                    if(!net_service.isScanning()) {
                        mHandler.sendEmptyMessage(UIMessageCode.UPDATE_TOOLBAR_SUBTITLE_TO_SCANNING);
                        item.setTitle("stop");
                        net_service.startScanner();
                        Toast.makeText(this,"start scanning",Toast.LENGTH_SHORT)
                                .show();
                    }
                    else{
                        mHandler.sendEmptyMessage(UIMessageCode.UPDATE_TOOLBAR_SUBTITLE_TO_FINISH);
                        net_service.stopScanning();
                        Toast.makeText(this,String.valueOf(net_service.scanner.isScanning()),Toast.LENGTH_SHORT)
                        .show();
                        item.setTitle("scan");
                    }
                    consume_result=true;

                break;
            case R.id.menu_item_lan_send:
                consume_result=true;
                //save targets and exit the activity
                Intent intent=new Intent();
                try {
                    InetAddress targetAddress=adapter.getSelectedTarget();
                    intent.putExtra("selected_device",targetAddress);
                    this.setResult(MainActivity.MessageCode.DEVICE_SELECTED,intent);
                } catch (UnknownHostException e) {
                    makeToast(e.getMessage());
                }
                finish();
                break;
            default:
                consume_result= super.onOptionsItemSelected(item);
                break;
        }
        return consume_result;

    }


    public void makeToast(String toast_text){
        this.mHandler.sendMessage(
                Request.obtain(FMGlobal.MAKE_TOAST,
                        toast_text  )
        );
    }
}
