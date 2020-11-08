package com.scut.filemanager.ui;

import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;

import java.io.IOException;

public class TabViewController implements AdapterView.OnItemClickListener,com.scut.filemanager.util.KeyDownEventHandler
{

    /*
    @Description: 管理MainFrame(也叫MainActivity)所inflate 的对象的状态.
    监听处理来自装载布局所产生的事件请求，

     */
    private FileHandle current=null;
    private FileHandle parent=null;
    private Service service=null;
    private View tabView=null;
    private ItemAssembler adapter;

    public TabViewController(Service svc, View view) throws Exception {
        service=svc;
        tabView=view;
        LoadFirstTab();
    }


    public void LoadFirstTab() throws Exception {
        current=service.getRootDirFileHandle();
        adapter=new ItemAssembler(service.getContext(),current,R.layout.list_item); //reflection is used to get context of app by service
        ListView listView=(ListView)tabView;
        listView.setAdapter(adapter);
        Log.d("TabViewManager","load first tab");

    }

    /*
    @Description:
    Callback method to be invoked when an item in this AdapterView has been clicked.
    Implementers can call getItemAtPosition(position) if they need to access the data associated with the selected item.

    @Parameters
    parent	AdapterView: The AdapterView where the click happened.
    view	View: The view within the AdapterView that was clicked (this will be a view provided by the adapter)
    position	int: The position of the view in the adapter.
    id	long: The row id of the item that was clicked.
     */

    @Override
    public void onItemClick(AdapterView<?> parentView, View view, int position, long id) {
        //获取到constrainLayout: View
        FileHandle handle=(FileHandle)adapter.getItem(position); //a tricky way that modifies the content within adapter
        if(handle.isDirectory()){
                adapter.setFolder(handle);
                parent=current;
                current=handle;
        }
    }


    public boolean onReturnKeyDown(AdapterView<?> parentView) throws IOException {
       if(parent!=null){
           current=parent;
           if(!current.isRoot()){
               parent=current.getParentFileHandle();
           }
           else{
               parent=null;
           }
           adapter.setFolder(current);
           return true;
       }
       else{
           return false;
       }
    }



    @Override
    public boolean onKeyDownEventHandleFunction(AdapterView<?> parentView, int keyCode, KeyEvent keyEvent) throws IOException {
        return onReturnKeyDown(parentView);
    }
}
