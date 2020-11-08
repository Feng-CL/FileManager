package com.scut.filemanager;
import android.content.Context;
import android.util.Log;
import android.view.ViewStub;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.scut.filemanager.core.*;
import com.scut.filemanager.ui.TabViewController;
import com.scut.filemanager.util.KeyDownEventHandler;

import java.io.IOException;

public class MainController {

    public Service service=null;

    private Context context=null;
    private AppCompatActivity main_activity=null;
    private TabViewController tabViewController =null;
    /*这里的监听器目前只有一个，到后期通过保存监听器引用的队列，通过主控器对象
    * 获取来自MainFrame的事件，根据给定条件（状态）再分发事件到每一个拥有自定义监听器接口（协议）
    * 的对象上进行处理*/

    private KeyDownEventHandler mlistener=null;


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

   // @RequiresApi(api = Build.VERSION_CODES.O)
    public void init() throws Exception {
        ViewStub viewStub= (ViewStub)main_activity.findViewById(R.id.viewStub_main);
        viewStub.setLayoutResource(R.layout.list_view_basic_for_files);
        ListView listView=(ListView)viewStub.inflate(); viewStub.setVisibility(ViewStub.VISIBLE);
        tabViewController =new TabViewController(service,listView);
        listView.setOnItemClickListener(tabViewController);
        setKeyDownEventListener(tabViewController);
        Log.d("MainController","init successfully");
    }

    public void setKeyDownEventListener(com.scut.filemanager.util.KeyDownEventHandler listener){
        mlistener=listener;
    }

    public boolean handleKeyDownEvent_callback() throws IOException {
        return mlistener.onKeyDownEventHandleFunction(null,0,null);
    }

}
