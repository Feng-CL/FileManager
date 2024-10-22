package com.scut.filemanager.main.activity;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.scut.filemanager.FileManager;
import com.scut.filemanager.R;
import com.scut.filemanager.core.net.NetService;
import com.scut.filemanager.main.MainActivity;
import com.scut.filemanager.ui.adapter.DeviceListViewAdapter;
import com.scut.filemanager.ui.transaction.MessageBuilder;
import com.scut.filemanager.util.protocols.WifiStateChangeListener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/*
@Description: 该活动主要用于处理选择发送设备的，把选择的索引发送给原来的activity
 */
public class DeviceSelectActivity extends AppCompatActivity
implements View.OnClickListener, WifiStateChangeListener
{

    //UI outlets:
    Toolbar toolbar;
    ListView listView;
    TextView textView_errorMsg;
    Menu menu;
    //adapter for list view
    public DeviceListViewAdapter adapter;


    //NetService reference:
    NetService net_service;

    //state maintain
    private boolean device_list_change_lock =true;
    private boolean menuBtnEnabled;
    private boolean wifiStateChangeRespondFlag_idle=false;

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
                       refreshTimer=null;
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
                    if(!adapter.contain(itemInMsg)&&!isDeviceListLocked()){
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
               case FileManager.MAKE_TOAST:
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
        net_service= FileManager.netService;
        net_service.bindDeviceSelectActivity(this);
        setContentView(R.layout.activity_lan_sender);

        //retrieve UI outlets:
        this.listView=(ListView)findViewById(R.id.listview_for_displaying_devices);
        this.toolbar=(Toolbar) findViewById(R.id.my_toolbar);
        this.textView_errorMsg=findViewById(R.id.textview_lansender_err_msg);
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

        adapter=new DeviceListViewAdapter(getLayoutInflater(),this);
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
        Log.d("deviceSelectActivity", "onStart: "+net_service.getWifiStatus());
        //check network state
        if(net_service.getWifiStatus()!=NetService.NetStatus.WIFI_CONNECTED){
            String err_msg="Current wifi status is "+net_service.getWifiStatus().name()+" please check your network setting";
            this.textView_errorMsg.setText(err_msg);
            if(menu==null) {
                menuBtnEnabled = false;
            }
            else{
                for(int i=0;i<menu.size();i++){
                    menu.getItem(i).setEnabled(false);
                }
            }

        }
        else{
            textView_errorMsg.setText("");
            if(menu==null) {
                menuBtnEnabled = true;
            }
            else {
                for(int i=0;i<menu.size();i++){
                    menu.getItem(i).setEnabled(true);
                }
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        wifiStateChangeRespondFlag_idle=true;
        this.net_service.unBindDeviceSelectActivity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_lan_sender,menu);
        this.menu=menu;
        if(!menuBtnEnabled){
            for(int i=0;i<menu.size();i++){
                menu.getItem(i).setEnabled(false);
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        int tag= (int) v.getTag();
        adapter.mark(tag);
    }

    @Override
    public void onWifiStateChange(NetService.NetStatus wifi_status) {
        Log.d(this.getClass().getName(),"onWifiStateChange "+ wifi_status.name());
        if(wifi_status.equals(NetService.NetStatus.WIFI_CONNECTED)){
            menuBtnEnabled=true;
            for(int i=0;i<menu.size();i++){
                menu.getItem(i).setEnabled(menuBtnEnabled);
            }
        }
        else {
            menuBtnEnabled=false;
            for(int i=0;i<menu.size();i++){
                menu.getItem(i).setEnabled(menuBtnEnabled);
            }
            String tip;
            switch (wifi_status){
                case WIFI_ENABLED:
                    tip="Wifi is enabled, but not connected, please check your network configuration";
                    break;
                case WIFI_ENABLING:
                    tip="Wifi is enabling";
                    break;
                case WIFI_DISABLING:
                    tip="Wifi is disabling";
                    break;
                default:
                    tip="Current wifi state is "+wifi_status.name().concat(", please check your network configuration");
                    break;
            }
            this.displayErrorMessage(tip);
        }
    }

    @Override
    public boolean isIdle() {
        return wifiStateChangeRespondFlag_idle;
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
                    if(isDeviceListLocked()){
                        lockDeviceListRefresh(false);
                        mHandler.sendEmptyMessage(UIMessageCode.UPDATE_TOOLBAR_SUBTITLE_TO_SCANNING);
                        item.setTitle("stop");
                        adapter.clearItems();
                        Toast.makeText(this,"start scanning",Toast.LENGTH_SHORT)
                                .show();
                    }
                    else{
                        mHandler.sendEmptyMessage(UIMessageCode.UPDATE_TOOLBAR_SUBTITLE_TO_FINISH);
                        lockDeviceListRefresh(true);
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

    public void displayErrorMessage(String err){ //只能在ui线程中调用该方法
        textView_errorMsg.setText(err);
    }

    public void makeToast(String toast_text){
        this.mHandler.sendMessage(
                MessageBuilder.obtain(FileManager.MAKE_TOAST,
                        toast_text  )
        );
    }

    private void lockDeviceListRefresh(@NonNull boolean lock){
        this.device_list_change_lock=lock;
    }

    private boolean isDeviceListLocked(){
        return this.device_list_change_lock;
    }
}
