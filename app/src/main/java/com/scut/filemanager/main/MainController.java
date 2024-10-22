package com.scut.filemanager.main;

import android.content.Context;
import android.util.Log;
import android.view.ViewStub;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.UiThread;

import com.scut.filemanager.R;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.net.InquirePacket;
import com.scut.filemanager.core.net.NetService;
import com.scut.filemanager.ui.controller.TabDirectoryViewController;
import com.scut.filemanager.ui.transaction.MessageBuilder;
import com.scut.filemanager.util.protocols.KeyDownEventHandler;

import java.io.IOException;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class MainController {

    //context members
    public Service service=null;
    public NetService netService=null;
    private Context context=null;
    private MainActivity main_activity=null;
    //--------------------------------------------

    //controller it manages
    private TabDirectoryViewController tabDirectoryViewController =null;
    //--------------------------------------------
    /*这里的监听器目前只有一个，到后期通过保存监听器引用的队列，通过主控器对象
    * 获取来自MainFrame的事件，根据给定条件（状态）再分发事件到每一个拥有自定义监听器接口（协议）
    * 的对象上进行处理*/
    //listeners for callback
    private KeyDownEventHandler mKeyDownEventListener =null;
    //--------------------------------------------

    //other usage members
    private boolean confirmExitApp=false;
    //--------------------------------------------
    public MainController(){

    }

    public boolean startService(android.app.Activity app_context) {
        service = Service.getInstance(app_context);
        context = app_context;
        main_activity = (MainActivity) context;


        //check Service status
        if (service.getStatus() == Service.SERVICE_STATUS.OK) {
            return true;
        } else {
            return false;
        }


    }

    public String getServiceStatus(){
        return service.getStatus().name();
    }

    public void startNetService() throws SocketException {
        if(service!=null){
            netService=NetService.getInstance(service);
        }
        netService.setMainController(this);
        netService.startBoardCaster();
        netService.startScanner();
    }

    @UiThread
    public void init() throws Exception {
        ViewStub viewStub= (ViewStub)main_activity.findViewById(R.id.viewStub_for_listView);
        viewStub.setLayoutResource(R.layout.list_view_basic_for_files);
        ListView listView=(ListView)viewStub.inflate(); viewStub.setVisibility(ViewStub.VISIBLE);
        tabDirectoryViewController =new TabDirectoryViewController(service,null,main_activity.findViewById(R.id.layout_tabViewContainer),listView);
        setKeyDownEventListener(tabDirectoryViewController);

        Log.d("MainController","init successfully");
    }

    public void setKeyDownEventListener(KeyDownEventHandler listener){
        mKeyDownEventListener =listener;
    }

    public boolean handleKeyDownEvent_callback() throws IOException {
        boolean mlistener_consume_result= mKeyDownEventListener.onKeyDownEventHandleFunction(null,0,null);
        //无返回键的监听器处理返回事件，此时告知用户需要按两下
        if(!mlistener_consume_result){
            //not confirm, but make confirmExitApp true within 3 seconds
            //so that second call of this callback will notify upper to exit
            if(!confirmExitApp) {
                confirmExitApp = true;
                Toast exit_tip_toast = Toast.makeText(service.getContext(), "press again to exit", Toast.LENGTH_SHORT);
                exit_tip_toast.show();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        confirmExitApp = false;
                        Log.d("MainCtrl.timer.schedule", "confirmExitApp set to "+ false);
                    }
                }, 3000);
                return true;
            }
            else{
                return false; //event is not consumed, dispatch to upper
            }

        }
        return mlistener_consume_result;
    }

    public TabDirectoryViewController getTabDirectoryViewController(){
        return tabDirectoryViewController;
    }

    public void InvokeReceiveInquireDialog(InquirePacket packet){
        main_activity.getHandler().sendMessage(
                MessageBuilder.obtain(MainActivity.MessageCode.INVOKE_RECEIVE_INQUIRY_DIALOG,packet)
        );
    }



}
