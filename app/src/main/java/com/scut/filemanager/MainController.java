package com.scut.filemanager;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.TabViewController;
import com.scut.filemanager.util.protocols.KeyDownEventHandler;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainController {

    //context members
    public Service service=null;
    private Context context=null;
    private AppCompatActivity main_activity=null;
    //--------------------------------------------

    //controller it manages
    private TabViewController tabViewController =null;
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

    public boolean startService(Context app_context) {
        service = Service.getInstance(app_context);
        context = app_context;
        main_activity = (AppCompatActivity) context;

        //check Service status
        if (service.getStatus() == Service.SERVICE_STATUS.OK) {
            return true;
        } else {
            return false;
        }


    }


    public void init() throws Exception {
        ViewStub viewStub= (ViewStub)main_activity.findViewById(R.id.viewStub_for_listView);
        viewStub.setLayoutResource(R.layout.list_view_basic_for_files);
        ListView listView=(ListView)viewStub.inflate(); viewStub.setVisibility(ViewStub.VISIBLE);
        tabViewController =new TabViewController(service,null,main_activity.findViewById(R.id.layout_tabViewContainer),listView);
        setKeyDownEventListener(tabViewController);

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

}
