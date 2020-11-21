package com.scut.filemanager.ui;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;

import java.io.IOException;


public class TabViewController implements AdapterView.OnItemClickListener,com.scut.filemanager.util.KeyDownEventHandler
    , AdapterView.OnItemLongClickListener
{

    /*
    @Description: 管理MainFrame(也叫MainActivity)所inflate 的对象的状态.
    监听处理来自装载布局所产生的事件请求，

     */
    private OperationBarController operationBarController;
    private FileHandle current=null;
    private FileHandle parent=null;
    private Service service=null;
    private View tabView=null;  //该控制器控制的视图
    private SimpleListItemAssembler adapter;


    protected OPERATION_STATE operation_state=OPERATION_STATE.STATIC;

    //定义当前视图下的操作状态
    enum OPERATION_STATE{
        STATIC,
        SCROLLING,
        SELECTING, //选择状态
        COPY,
        CUT,
        OTHER //更多状态未定义
    }


    public TabViewController(Service svc, View view,OperationBarController barController) throws Exception {
        service=svc;
        tabView=view;
        operationBarController=barController;
        LoadFirstTab();
    }


    public void LoadFirstTab() throws Exception {
        current=service.getRootDirFileHandle();
        //reflection is used to get context of app by service
        android.view.LayoutInflater layoutInflater=(LayoutInflater) service.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
        adapter=new SimpleListItemAssembler(current,R.layout.list_item,layoutInflater);
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
        if(operation_state==OPERATION_STATE.STATIC) {
            //获取到constrainLayout: View
            FileHandle handle = (FileHandle) adapter.getItem(position); //a tricky way that modifies the content within adapter
            if (handle.isDirectory()) {
                adapter.setFolder(handle);
                parent = current;
                current = handle;
            }
        }
        else if( operation_state==OPERATION_STATE.SELECTING){
            Log.d("tabViewController","item click at SELECTING state");
            CheckBox checkBox=view.findViewById(R.id.item_checkbox);
            boolean checkState=checkBox.isChecked();
            checkBox.setChecked(!checkState);
            adapter.setSelectedAtItem(position,!checkState);
        }
    }



    public boolean onReturnKeyDown(AdapterView<?> parentView) throws IOException {
       if(operation_state!=OPERATION_STATE.SELECTING) {
           if (parent != null) {
               current = parent;
               if (!current.isRoot()) {
                   parent = current.getParentFileHandle();
               } else {
                   parent = null;
               }
               adapter.setFolder(current);
               return true;
           } else {
               return false; //该事件为被消费，但这里缺少dispatch 函数
           }
       }
       else {
            clearOperationState();
            adapter.refreshSelectedTableToAllUnSelected();
            adapter.updateCheckBoxDisplayState(View.INVISIBLE);
            return true;
       }
    }



    @Override
    public boolean onKeyDownEventHandleFunction(AdapterView<?> parentView, int keyCode, KeyEvent keyEvent) throws IOException {
        return onReturnKeyDown(parentView);
    }

    /*
    @Description: 长按item的回调
    public abstract boolean onItemLongClick (AdapterView<?> parent,
                View view,
                int position,
                long id)
                Parameters
    parent	AdapterView: The AbsListView where the click happened
    view	View: The view within the AbsListView that was clicked
    position	int: The position of the view in the list
    id	long: The row id of the item that was clicked
     */

    /*
    @Description: 检查当前tabView 的状态，并设置所有checkBox 为可视状态
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Log.d(this.getClass().getName(),"on item long click event");
        if(operation_state==OPERATION_STATE.STATIC){
            //更新当前状态为SELECTING
            operation_state=OPERATION_STATE.SELECTING;
            //显示所有checkbox
            adapter.updateCheckBoxDisplayState(View.VISIBLE);
            adapter.setSelectedAtItem(i,true);
            //选中当前项的checkbox
            CheckBox checkBoxLongClicked=view.findViewById(R.id.item_checkbox);
            checkBoxLongClicked.setChecked(true);
            //Log.d("tabViewController","focus Status: "+checkBoxLongClicked.isFocused());
            return true;
        }
        return false;
    }

    /*
    刷新选中表, 当进入新视图时,确保选中表足够大，并把默认状态置为unchecked
     */


    private void clearOperationState(){
        operation_state=OPERATION_STATE.STATIC;
    }




}
