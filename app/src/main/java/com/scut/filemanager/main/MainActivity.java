package com.scut.filemanager.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;


import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.concurrent.SharedThreadPool;
import com.scut.filemanager.main.activity.DeviceSelectActivity;
import com.scut.filemanager.FileManager;
import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.net.FileNodeWrapper;
import com.scut.filemanager.core.net.InquirePacket;
import com.scut.filemanager.main.fragment.NavigationDrawerFragment;
import com.scut.filemanager.ui.dialog.NotifyDialogDelegate;
import com.scut.filemanager.ui.dialog.SingleLineInputDialogDelegate;
import com.scut.filemanager.ui.protocols.AbstractDialogCallBack;
import com.scut.filemanager.ui.transaction.FileTransferTransactionMiddleWare;
import com.scut.filemanager.ui.transaction.MIME_MapTable;
import com.scut.filemanager.ui.transaction.Request;
import com.scut.filemanager.util.FMFormatter;

public class MainActivity extends AppCompatActivity

{
    //ref controller
    MainController controller=null;

    //UI Outlets
    androidx.appcompat.widget.Toolbar toolbar;
    NavigationDrawerFragment navFragment;

    //function member
    private GestureDetector mGestureDetector;
    boolean navFragment_outState =false;

    //listener Impl
    View.OnClickListener navigation_button_onclick=new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if(navFragment!=null){
                if(!navFragment_outState) {
                    navFragment.pullNavigation();
                    navFragment_outState =true;
                }
            }
        }
    };



    GestureDetector.OnGestureListener mOnGestureListener=new GestureDetector.OnGestureListener() {

        //Use-Case:
        /*
        UC1: 快速划过，隐藏导航栏
        UC2: 用户长按屏幕，在松开之前，都要拦截事件，不能往下传递, 此时手指可能会左右滑动，这是需要带动 NavigationBar Fragment 一同移动
        UC3: 点击非NavFragment区域时需要将其隐藏。
        UC4: 上一步事件也需要拦截。
         */

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent ev1, MotionEvent ev2, float v0, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v0, float v1) {
            return false;
        }
    };


    //handler
    Handler mHandler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case FileManager.MAKE_TOAST:
                    String text="class cast exception";
                    if(msg.obj instanceof String){
                        text= (String) msg.obj;
                    }
                    Toast.makeText(MainActivity.this,text,Toast.LENGTH_SHORT).show();
                    break;
                case MessageCode.INVOKE_RECEIVE_INQUIRY_DIALOG:
                    final InquirePacket packet= (InquirePacket) msg.obj;
                    String notification=packet.ip.getHostAddress().concat(" is sending files to you, do you want to receive? " +
                            "the files' size is ").concat(
                                   FMFormatter.getSuitableFileSizeString(
                                           ((FileNodeWrapper)(packet.obj)).getTotalSize()
                                    )
                    );
                    NotifyDialogDelegate delegate=new NotifyDialogDelegate(MainActivity.this, new AbstractDialogCallBack() {
                        @Override
                        public void onDialogOk(DialogInterface dialog) {
                            //invoke location picker dialog and receiver task transaction middle ware
                            FileTransferTransactionMiddleWare middleWare=new FileTransferTransactionMiddleWare(
                                    FileTransferTransactionMiddleWare.TaskType.RECEIVE,MainActivity.this,
                                    controller.netService
                            );
                            middleWare.executeReceiveTask(packet,controller.service);
                            dialog.dismiss();
                        }

                        @Override
                        public void onDialogCancel(DialogInterface dialog) {
                            controller.netService.refuseAndSendNACK(packet.ip);
                            dialog.cancel();
                        }

                    }, MainActivity.this.getResources().getString(R.string.dialogTitle_receiveFiles), notification);
                    delegate.show();
                default:
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        TextView tv=(TextView)findViewById(R.id.textview3);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar=(Toolbar)findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar); //尽管存在于布局中，但toolbar 是不显示的,需要通过setSupportActionBar显示指定。
        toolbar.setNavigationOnClickListener(navigation_button_onclick);

        navFragment= (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main_navigation);

        int CHECK_PERMISSION_STORAGE=getPackageManager().checkPermission(MainActivity.PERMISSION_STORAGE[1],getPackageName());
        if(CHECK_PERMISSION_STORAGE!= PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(MainActivity.PERMISSION_STORAGE, MainActivity.REQUEST_PERMISSION_CODE);
        }
        else {
            this.initialization_process();
        }



    }

    public void loadFragmentDirectoryView(FileHandle dir){
         {
             //"stub code here "
             controller.getTabViewController().setDisplayFolder(dir);
         }
    }

    private void initialization_process(){
        controller = new MainController();
        boolean result = controller.startService(this); //which can be an  AsyncTask
        if (result) {
            try {
                controller.startNetService();
                registerReceiver(controller.netService.getWiFiConnectChangeBroadCastReceiver(),
                        controller.netService.getWiFiConnectChangeBroadCastReceiver().getIntentFilter());
                this.performAsyncTask();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            //dispatchTouchEvent()
            try {
                controller.init();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            Toast.makeText(this, controller.getServiceStatus(), Toast.LENGTH_SHORT)
                    .show();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        //启动前准备,暂时先不对闲置对象进行管理
        Log.d("deviceName:",android.os.Build.MODEL);

        FileManager.Default_shortAnimTime=getResources().getInteger(android.R.integer.config_shortAnimTime);
        FileManager.Default_longAnimTime=getResources().getInteger(android.R.integer.config_longAnimTime);


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


    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean permission_granted_totally=true;
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
                makeToast("申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
                if(grantResults[i]!= PackageManager.PERMISSION_GRANTED) {
                    permission_granted_totally =false;
                }
            }
            if(permission_granted_totally){
                this.initialization_process();
            }
        }
    }


    //一个临时的onKeyDownListener MainActivity
    @Override
    public boolean onKeyDown(int keycode,KeyEvent k_ev){

        if(keycode==KeyEvent.KEYCODE_BACK){
            try {
                if(this.navFragment_outState){
                    navFragment.pushNavigation();
                    this.navFragment_outState=false;
                }
                else {
                    boolean comsume_result = controller.handleKeyDownEvent_callback();
                    if (!comsume_result) {
                        this.finish();
                    }
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
                invokeDeviceSelectActivity();
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
                    FileHandle location=controller.getTabViewController().getCurrentLocationFileHandle();
                    SingleLineInputDialogDelegate delegate=new SingleLineInputDialogDelegate(SingleLineInputDialogDelegate.DialogType.NEW_DIRECTORY,
                            controller.getTabViewController(),controller.getTabViewController());
                    delegate.showDialog();
                }
                consume=false;
                break;
            case R.id.main_menu_item_new:
                if(controller.getTabViewController()!=null){
                    FileHandle location=controller.getTabViewController().getCurrentLocationFileHandle();
                    SingleLineInputDialogDelegate delegate=new SingleLineInputDialogDelegate(SingleLineInputDialogDelegate.DialogType.NEW_FILE,
                            controller.getTabViewController(),controller.getTabViewController());
                    delegate.showDialog();
                }
                consume=false;
                break;
            default:
                consume=super.onOptionsItemSelected(item);
                break;
        }
        return consume;
    }

    private void setUpNavFragmentOnItemClickListener(){
        navFragment.setListenerForExpandableListView(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                boolean result=true;
                switch (groupPosition) {
                    case 0: {
                        switch (childPosition) {
                            case 0: //INTERNAL_STORAGE
                                loadFragmentDirectoryView(controller.service.getStorageDirFileHandle());
                                navFragment_outState=false;
                                navFragment.pushNavigation();
                                break;
                            case 1: //EXTERNAL_STORAGE
                                loadFragmentDirectoryView(controller.service.getSDCardRootDirectoryFileHandle());
                                navFragment_outState=false;
                                navFragment.pushNavigation();
                                break;
                            default:
                                result=false;
                                Log.d("navig onclick", "onItemClick: " + id);
                                break;
                        }
                        break;
                    }
                    default:
                        Log.d("navig onclick", "onItemClick: " + id);
                        result=false;
                        break;
                }
                Log.d("onChildClick",String.valueOf(groupPosition));
                return result;
            }

        });
    }

    private void performAsyncTask(){
        Runnable task=new Runnable() {
            @Override
            public void run() {
                //mGestureDetector=new GestureDetector(MainActivity.this,mOnGestureListener);
                setUpNavFragmentOnItemClickListener();
            }
        };
        SharedThreadPool.getInstance().executeTask(task,SharedThreadPool.PRIORITY.HIGH);
    }

    private void invokeDeviceSelectActivity(){
        Intent intent=new Intent(this, DeviceSelectActivity.class);
        selectedFiles=controller.getTabViewController().getSelectedFileHandles();
        FileManager.netService=controller.netService;
        this.startActivityForResult(intent,MessageCode.SEND_BY_LAN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case MessageCode.SEND_BY_LAN:{
                if(resultCode==MessageCode.DEVICE_SELECTED){
                    //extract data from intent and invoke process procedure
                    InetAddress targetAddress= (InetAddress) data.getSerializableExtra("selected_device");
                    FileTransferTransactionMiddleWare middleWare=new FileTransferTransactionMiddleWare(
                            FileTransferTransactionMiddleWare.TaskType.SEND,this,controller.netService
                    );
                    middleWare.executeSendTask(targetAddress,selectedFiles);
                }
            }
            break;
            default:
                break;
        }
    }

    public static void openFile(Context context, String file) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);

            Uri fileUri=FileProvider.getUriForFile(context,"com.scut.filemanager",new File(file));
            intent.setDataAndType(fileUri, MIME_MapTable.getMIMEType(file));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
            Intent.createChooser(intent, "you want to open it with?");
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "sorry cannot find correspond handler ", Toast.LENGTH_SHORT).show();
        }
    }

    public Handler getHandler(){
        return this.mHandler;
    }


    public static List<FileHandle> selectedFiles;

    public static class MessageCode{
        public static final int SEND_BY_LAN=10;
        public static final int INVOKE_RECEIVE_INQUIRY_DIALOG=11;
        public static final int ACTIVITY_CANCELED=20;
        public static final int DEVICE_SELECTED=21;
    }

    private void makeToast(String toast){
        this.mHandler.sendMessage(
                Request.obtain(FileManager.MAKE_TOAST,toast)
        );
    }
}