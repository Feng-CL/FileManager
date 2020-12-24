package com.scut.filemanager.ui;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.adapter.SimpleListViewItemAssembler;
import com.scut.filemanager.util.protocols.DisplayFolderChangeResponder;
import com.scut.filemanager.util.protocols.KeyDownEventHandler;

import java.io.IOException;


public class TabViewController extends BaseController implements AdapterView.OnItemClickListener, KeyDownEventHandler
    , AdapterView.OnItemLongClickListener
{

    /*
    @Description: 管理MainFrame(也叫MainActivity)所inflate 的对象的状态.
    监听处理来自装载布局所产生的事件请求，

     */
    private OperationBarController operationBarController; //操作栏控制器
    private LocationBarController locationBarController; //地址栏控制器
    private FileHandle current=null; //当前视图引用的文件夹
    private FileHandle parent=null; //当前视图引用文件夹的父文件夹
    private boolean isReachRoot;    //判断当前视图是否应该继续返回键的事件
    private Service service=null;
    private View tabView=null;  //该控制器控制的视图
    private SimpleListViewItemAssembler adapter; //视图的数据来源
    private ProgressBar progressCircle;
    private boolean[] loadingLock={true,true,true}; //用于同步一些行为

    static private FileHandle[] SuperFolder=new FileHandle[2]; //特殊的文件句柄，用于显示内外存储的文件夹

    protected OPERATION_STATE operation_state=OPERATION_STATE.STATIC; //标记当前状态

    @Override
    public Context getContext() {
        return service.getContext();
    }

    @Override
    public Handler getHandler() {
        return mHandler;
    }

    //定义当前视图下的操作状态
    enum OPERATION_STATE{
        STATIC,
        SCROLLING,
        SELECTING, //选择状态
        COPY,
        CUT,
        OTHER //更多状态未定义
    }

//    Handler mHandler=new Handler(Looper.getMainLooper()){
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            super.handleMessage(msg);
//
//        }
//    };


    public TabViewController(Service svc, ViewGroup parentView, View viewManaged) throws Exception {
        service=svc;
        this.parentView=parentView;
        tabView=viewManaged;
        locationBarController=new LocationBarController(null,(ViewStub)parentView.findViewById(R.id.viewStub_for_locationBar),this);
        operationBarController=new OperationBarController((ViewStub)parentView.findViewById(R.id.rootview_for_operationBar));
        progressCircle=parentView.findViewById(R.id.progressbar_loading);
        initStaticMember();
        LoadFirstTab();
    }




    public void LoadFirstTab() throws Exception {
        current=service.getStorageDirFileHandle();
        isReachRoot=false;
        adapter=new SimpleListViewItemAssembler(current,this);
        ListView listView=(ListView)tabView;
        listView.setAdapter(adapter);
        registerFolderChangedResponder(locationBarController);

        Log.d("TabViewManager","load first tab");
        locationBarController.setFolderAndUpdateView(current);

    }

    public LayoutInflater getLayoutInflater(){
        android.view.LayoutInflater layoutInflater=(LayoutInflater) service.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
        return layoutInflater;
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
    public void updateProgressBarVisibility(int visibility){
        progressCircle.setVisibility(visibility);
    }

    @Override
    public void onItemClick(AdapterView<?> parentView, View view, int position, long id) {
        if(operation_state==OPERATION_STATE.STATIC) {
            //获取到constrainLayout: View
            FileHandle handle = (FileHandle) adapter.getFileHandleAtPosition(position); //a tricky way that modifies the content within adapter
            if (handle.isDirectory()) {
                adapter.setFolder(handle);
                parent = current;
                current = handle;
                isReachRoot=false;
            }
            else{
                //invoke file viewers
            }
        }
        else if( operation_state==OPERATION_STATE.SELECTING){
            Log.d("tabViewController","item click at SELECTING state");
            CheckBox checkBox=view.findViewById(R.id.item_checkbox);
            boolean checkState=checkBox.isChecked();
            checkBox.setChecked(!checkState);

        }
    }



    public boolean onReturnKeyDown(AdapterView<?> parentView) throws IOException {
       if(operation_state!=OPERATION_STATE.SELECTING) {

            if(!isReachRoot){
                if(current.isAndroidRoot()){
                    isReachRoot=true;
                    adapter.setListAssembled(SuperFolder);
                    current=FileHandle.superHandle;
                    parent=null;
                }
                else{
                    current=parent;
                    parent=current.getParentFileHandle();
                    adapter.setFolder(current);
                }
                return true;
            }
            else{
                //这里需要一个计时任务
                return false; //该事件未被消费，上层应通过false让上层继续处理
            }
       }
       else {
            clearOperationState();
            //adapter.updateCheckBoxDisplayState(View.INVISIBLE);
            return true;
       }
    }

    public void registerFolderChangedResponder(DisplayFolderChangeResponder responder){
        adapter.registerFolderChangedResponder(responder);
    }


    public void setDisplayFolder(@NonNull FileHandle folder){
        current=folder;
        if(current.isAndroidRoot()){
            isReachRoot=true;
        }
        parent=folder.getParentFileHandle();
        adapter.setFolder(current);
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
            //adapter.updateCheckBoxDisplayState(View.VISIBLE);

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

    private void initStaticMember(){
        if(service!=null){
            SuperFolder[0]=service.getStorageDirFileHandle();
            if(service.getSdcardStatus()==Service.SERVICE_STATUS.SDCARD_MOUNTED){
                SuperFolder[1]=service.getSDCardRootDirectoryFileHandle();
            }
            else{
                SuperFolder[1]=null;
            }
        }
    }





}
