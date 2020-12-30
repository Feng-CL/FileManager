package com.scut.filemanager.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.scut.filemanager.FMGlobal;
import com.scut.filemanager.MainActivity;
import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.concurrent.SharedThreadPool;
import com.scut.filemanager.core.internal.DeleteTaskMonitor;
import com.scut.filemanager.ui.adapter.SimpleListViewItemAssembler;
import com.scut.filemanager.ui.dialog.LocationPickDialogDelegate;
import com.scut.filemanager.ui.dialog.NotifyDialog;
import com.scut.filemanager.ui.dialog.SingleLineInputDialogDelegate;
import com.scut.filemanager.ui.protocols.DialogCallBack;
import com.scut.filemanager.ui.protocols.InputConfirmCallBack;
import com.scut.filemanager.ui.protocols.LocationPickerCallback;
import com.scut.filemanager.ui.protocols.SingleLineInputDialogCallBack;
import com.scut.filemanager.ui.transaction.CopyTransactionProxy;
import com.scut.filemanager.ui.transaction.MIME_MapTable;
import com.scut.filemanager.ui.transaction.MoveTransactionProxy;
import com.scut.filemanager.util.protocols.DisplayFolderChangeResponder;
import com.scut.filemanager.util.protocols.KeyDownEventHandler;

import java.io.IOException;
import java.util.List;


public class TabViewController extends BaseController implements AdapterView.OnItemClickListener, KeyDownEventHandler
    , AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener
    , SingleLineInputDialogCallBack, LocationPickerCallback
{

    /*
    @Description: 管理MainFrame(也叫MainActivity)所inflate 的对象的状态.
    监听处理来自装载布局所产生的事件请求，

     */
    private OperationBarController operationBarController; //操作栏控制器
    private LocationBarController locationBarController; //地址栏控制器

    //filemanager.core.*
    private Service service=null;
    private FileHandle current=null; //当前视图引用的文件夹
    private FileHandle parent=null; //当前视图引用文件夹的父文件夹

    private boolean isReachRoot;    //判断当前视图是否应该继续返回键的事件

    //UI outlets
    private ListView listViewInTab =null;  //该控制器控制的视图
    private View tabView=null;
    private ProgressBar progressCircle;

    private SimpleListViewItemAssembler adapter; //视图的数据来源
    //private boolean[] loadingLock={true,true,true}; //用于同步一些行为

    static public FileHandle[] SuperFolder=new FileHandle[2]; //特殊的文件句柄，用于显示内外存储的文件夹


    private int selectedCount=0; //一个额外但很有用的记数变量，记录选中文件的数量
    protected OPERATION_STATE operation_state=OPERATION_STATE.STATIC; //标记当前状态
    protected OPERATION_STATE scrolling_state=OPERATION_STATE.STATIC; //标记滚动状态



    //定义当前视图下的操作状态
    enum OPERATION_STATE{
        STATIC,
        SCROLLING, SELECTING, //选择状态
        COPY, CUT, RENAME,NEW_FILE,MAKE_DIRECTORY,DELETE,
        MORE,
        OTHER //更多状态未定义
        ;
    }

//    Handler mHandler=new Handler(Looper.getMainLooper()){
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            super.handleMessage(msg);
//
//        }
//    };

    public TabViewController(Service svc, ViewGroup parentView, View viewManaged,ListView listView) throws Exception {
        super();
        service=svc;
        this.parentView=parentView;
        tabView=viewManaged;
        //设置监听
        listViewInTab=listView;
        listViewInTab.setOnItemClickListener(this);
        listViewInTab.setOnItemLongClickListener(this);
        listViewInTab.setOnScrollListener(this);


        locationBarController=new LocationBarController(null, (ViewStub) tabView.findViewById(R.id.viewStub_for_locationBar),this);
        operationBarController=new OperationBarController((ViewStub)tabView.findViewById(R.id.rootview_for_operationBar));
        operationBarController.setButtonOnClickListener(new OnOperationBarButtonClickedListener());

        progressCircle=tabView.findViewById(R.id.progressbar_loading);
        initStaticMember();
        LoadFirstTab();
    }

    public void LoadFirstTab() throws Exception {
        current=service.getStorageDirFileHandle();
        isReachRoot=false;
        adapter=new SimpleListViewItemAssembler(current,this);
        ListView listView=(ListView) listViewInTab;
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
                MainActivity.openFile(getContext(),handle.getAbsolutePathName());
            }
        }
        else if( operation_state==OPERATION_STATE.SELECTING){
            Log.d("tabViewController","item click at SELECTING state");
            CheckBox checkBox=view.findViewById(R.id.item_checkbox);
            boolean checkState=checkBox.isChecked();
            checkBox.setChecked(!checkState);
            if(checkState) { //当前已选中，此时按下按钮更新为不选中,计数-1
                selectedCount--;
            }
            else{
                selectedCount++;
            }
            SimpleListViewItemAssembler.ItemData item =(SimpleListViewItemAssembler.ItemData)adapter.getItem(position);
            item.isChecked=!checkState; //同时更新数据集中的内容

        }
        operationBarController.onOperationStatusChange(operation_state,selectedCount);
    }




    @Override
    public Context getContext() {
        return service.getContext();
    }

    @Override
    public Handler getHandler() {
        return mHandler;
    }

    @Override
    public Service getFileManagerCoreService() {
        return service;
    }

    @Override   //Callback method to be invoked while the list view or grid view is being scrolled.
    public void onScrollStateChanged(AbsListView absListView, int state) {
        if(state== AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL||state==AbsListView.OnScrollListener.SCROLL_STATE_FLING){
            scrolling_state=OPERATION_STATE.SCROLLING;
        }
        else {
            scrolling_state=OPERATION_STATE.STATIC;
        }
        //Log.i("tabViewScroll","scrollState: "+state);
        operationBarController.onScrollStateChange(scrolling_state);
    }

    @Override   //Callback method to be invoked when the list or grid has been scrolled.
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {

    }

    @Override
    public void onInputDialogCancelClick(String text) {
        operation_state=OPERATION_STATE.SELECTING;
    }

    public boolean onReturnKeyDown(AdapterView<?> parentView) throws IOException {
       if(operation_state==OPERATION_STATE.STATIC) {

            if(!isReachRoot){
                if(current.isAndroidRoot()){
                    isReachRoot=true;
                    adapter.setListAssembled(SuperFolder);
                    current=FileHandle.superHandle;
                    parent=null;
                }
                else{
                    current=parent;
                    if(current.isAndroidRoot()) {
                        parent = null;
                    }
                    else{
                        parent=current.getParentFileHandle();
                    }
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
            adapter.setCheckBoxVisibility(View.GONE);
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
            parent=null;
        }
        else {
            parent = folder.getParentFileHandle();
        }
        adapter.setFolder(current);
        selectedCount=0;
    }

    @Override
    public boolean onKeyDownEventHandleFunction(AdapterView<?> parentView, int keyCode, KeyEvent keyEvent) throws IOException {
        return onReturnKeyDown(parentView);
    }

    public FileHandle getSelectedFileHandle(){
        return adapter.getSelectedFile();
    }


    /*
        @Description:
        @Params:null
    */
    @Override
    public void onInputConfirmClicked(String text, int action) {
        //需要重置状态，刷新界面为STATIC

        boolean result = false;
        switch (action) {
            case SingleLineInputDialogDelegate.DialogType.NEW_DIRECTORY: {

                if (current.isInAndroidVolume()) { //验证位置
                    if (FileHandle.makeDirectory(current, text)) { //验证结果
                        Toast.makeText(getContext(), R.string.ToastHint_mkdir_true, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.ToastHint_mkdir_false, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), R.string.ToastHint_invalid_location_tip, Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case SingleLineInputDialogDelegate.DialogType.NEW_FILE: {
                if (current.isInAndroidVolume()) {
                    FileHandle newFile = new FileHandle(current,"/"+text);
                    result = newFile.create();
                    if (!result) {
                        Toast.makeText(getContext(), R.string.ToastHint_makeFile_false, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.ToastHint_makeFile_true, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), R.string.ToastHint_invalid_location_tip, Toast.LENGTH_SHORT).show();
                }

            }
            break;
            case SingleLineInputDialogDelegate.DialogType.RENAME: { //判断是否存在同名需要在回调发生前检测，即需要在对话框结束前检测
                //重复判断
                if(operation_state==OPERATION_STATE.RENAME){
                    FileHandle fileNeedRename=adapter.getSelectedFile();
                    if(fileNeedRename!=null){
                        result=fileNeedRename.rename(text);
                        if(result){
                            Toast.makeText(getContext(),R.string.ToastHint_rename_true,Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(getContext(),R.string.ToastHint_rename_false,Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(getContext(),R.string.ToastHint_unknown_error,Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
        clearOperationState();
        this.mHandler.sendEmptyMessage(UIMessageCode.NOTIFY_UPDATE_DATASET);
    }

    @Override
    public void onLocationPicked(FileHandle location) {
        if(operation_state==OPERATION_STATE.COPY){
            CopyTransactionProxy proxy=new CopyTransactionProxy(adapter.getSelectedFileHandles(),location.getAbsolutePathName(),this);
            proxy.execute();
            clearOperationState();
        }
        else if(operation_state==OPERATION_STATE.CUT){
            MoveTransactionProxy proxy=new MoveTransactionProxy(adapter.getSelectedFileHandles(),location.getAbsolutePathName(),this);
            proxy.execute();
            clearOperationState();
        }

    }

    @Override
    public void onLocationPickerDialogCancel(FileHandle currentLocation, boolean whetherNeedToUpdateView) {
        //state change here
        operation_state=OPERATION_STATE.SELECTING;
        if(whetherNeedToUpdateView){
            setDisplayFolder(current);
        }

    }




    public void setSelectAll(){
        adapter.setCheckBoxVisibility(View.VISIBLE);
        operation_state=OPERATION_STATE.SELECTING;
        selectedCount=adapter.getCount();
        operationBarController.onOperationStatusChange(operation_state,selectedCount);
        adapter.setSelectAll();
    }

    public FileHandle getCurrentLocationFileHandle(){
        return current;
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
            selectedCount=1;
            operation_state=OPERATION_STATE.SELECTING;
            //设置checkbox可见性
            adapter.setCheckBoxVisibility(View.VISIBLE);

            //选中当前项的checkbox
            CheckBox checkBoxLongClicked=view.findViewById(R.id.item_checkbox);
            checkBoxLongClicked.setVisibility(View.VISIBLE);
            checkBoxLongClicked.setChecked(true);

            //同时更新数据集
            SimpleListViewItemAssembler.ItemData item= (SimpleListViewItemAssembler.ItemData)adapter.getItem(i);
            item.isChecked=true;
            //Log.d("tabViewController","focus Status: "+checkBoxLongClicked.isFocused());

            operationBarController.onOperationStatusChange(operation_state,selectedCount);
            Log.d("selectCount", ": "+selectedCount );
            //事件被消费，返回true
            return true;
        }

        return false;
    }



    /*
    清空当前的操作状态。
     */
    private void clearOperationState(){
        operation_state=OPERATION_STATE.STATIC;
        adapter.setCheckBoxVisibility(View.INVISIBLE);
        adapter.setAllItemDataCheckedState(false);
        selectedCount=0;
        operationBarController.onOperationStatusChange(operation_state,selectedCount);
    }

    /*
        @Description:初始化静态变量
        @Params:
    */

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

    protected void setUpHandler(){
        this.mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what){
                    case UIMessageCode.NOTIFY_UPDATE_DATASET:
                        setDisplayFolder(current);
                        break;
                    case FMGlobal.MAKE_TOAST:
                        String toast_text= (String) msg.obj;
                        Toast.makeText(getContext(),toast_text ,Toast.LENGTH_SHORT)
                                .show();
                        break;
                    default:
                        break;
                }
            }
        };

    }

    public OPERATION_STATE getOperationState(){
        return operation_state;
    }



    //delete and detail
    protected class NotifyDialogCallBack
            implements DialogCallBack{

        @Override
        public void onDialogClose(boolean updateView) {
            if(updateView){
                clearOperationState();
                setDisplayFolder(current);
            }
        }

        @Override
        public void onDialogCancel() {

        }

        @Override
        public void onDialogHide() {

        }

        @Override
        public void onDialogNeutralClicked() {

        }

        @Override
        public void onDialogOk() {
            if(operation_state==OPERATION_STATE.DELETE){
                List<FileHandle> selectedFiles=adapter.getSelectedFileHandles();
                QueueTask queueTask=new QueueTask(selectedFiles);
                SharedThreadPool.getInstance().executeTask(queueTask, SharedThreadPool.PRIORITY.LOW);
            }
            else {
                operation_state=OPERATION_STATE.SELECTING;

            }
        }

        private class QueueTask implements Runnable{

            List<FileHandle> list;

            QueueTask(List<FileHandle> selectedFiles){
                this.list=selectedFiles;
            }

            @Override
            public void run() {
                boolean delete_result=true;
                for (FileHandle fileHandle :
                        list) {
                    delete_result&=fileHandle._deleteRecursively(null);
                }
                makeToast(delete_result);
                onDialogClose(true);
            }
        }

        protected void makeToast(boolean result){
            if(result){
                TabViewController.this.makeToast("delete successfully");
            }
            else {
                TabViewController.this.makeToast("delete failed");
            }
        }
    }




    /*
        @Description:处理操作栏按钮的内部监听处理类
    */

    private class OnOperationBarButtonClickedListener implements View.OnClickListener{

        /*
            @Description:
                        private int open_button_id = R.id.operation_button_open;  //1
                        private int copy_button_id = R.id.operation_button_copy;    //2
                        private int move_button_id = R.id.operation_button_move;    //3
                        private int rename_button_id = R.id.operation_button_rename; //4
                        private int delete_button_id = R.id.operation_button_delete; //5
                        private int more_button_id = R.id.operation_button_more; //6
                        private int cancel_button_id = R.id.operation_button_cancel; //7
                        private int paste_button_id = R.id.operation_button_paste; //8
                        private int newFolder_button_id = R.id.operation_button_newFolder; //9
        */

        @Override
        public void onClick(View view) {
            int button_tag= (int) view.getTag();
            switch (button_tag){
                case 1:
                    break;
                case 2: {
                    //需要通过operation_state来标记点击操作栏后进入的状态，否则回调函数无法知道结束后要做什么
                    operation_state = OPERATION_STATE.COPY;
                    LocationPickDialogDelegate locationPickDialogController = new LocationPickDialogDelegate(current,
                            TabViewController.this, TabViewController.this);
                    locationPickDialogController.showDialog();
                }
                    break;
                case 3:{
                    operation_state=OPERATION_STATE.CUT;
                    LocationPickDialogDelegate locationPickDialogController = new LocationPickDialogDelegate(current,
                            TabViewController.this, TabViewController.this);
                    locationPickDialogController.showDialog();
                }
                    break;
                case 4: {
                    operation_state = OPERATION_STATE.RENAME;
                    SingleLineInputDialogDelegate dialogDelegate = new SingleLineInputDialogDelegate(SingleLineInputDialogDelegate.DialogType.RENAME, TabViewController.this, TabViewController.this);
                    dialogDelegate.showDialog();
                }
                    break;
                case 5:{
                    operation_state=OPERATION_STATE.DELETE;
                    NotifyDialog notifyDialog=new NotifyDialog(NotifyDialog.dialogType.ACTION_DELETE,getContext(),new NotifyDialogCallBack());
                    notifyDialog.showDialog();
                }
                    break;
                case 6:{
                    operation_state=OPERATION_STATE.MORE;
                    NotifyDialogCallBack callBack= new NotifyDialogCallBack();
                    if(selectedCount>1){
                        NotifyDialog notifyDialog=new NotifyDialog(NotifyDialog.dialogType.ACTION_DETAIL_MULTI,getContext(),callBack);
                        notifyDialog.setDataSource(adapter.getSelectedFileHandles());
                        notifyDialog.showDialog();
                    }
                    else if(selectedCount==1) {
                        NotifyDialog notifyDialog = new NotifyDialog(NotifyDialog.dialogType.ACTION_DETAIL,getContext(),callBack);
                        notifyDialog.setDataSource(adapter.getSelectedFile());
                        notifyDialog.showDialog();
                    }
                }
                    break;
                case 7:{
                    operation_state=OPERATION_STATE.STATIC;
                    TabViewController.this.operationBarController.onOperationStatusChange(operation_state,TabViewController.this.selectedCount);
                }
                    break;
                case 8:{

                }
                    break;
                case 9:{

                }
                    break;
                default:
                    break;
            }
        }
    }





    public static class UIMessageCode {
       public  static final int NOTIFY_UPDATE_DATASET=0;
    }






}
