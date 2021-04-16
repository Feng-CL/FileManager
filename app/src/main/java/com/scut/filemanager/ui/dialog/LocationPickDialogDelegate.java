package com.scut.filemanager.ui.dialog;

/*
    @Description:由该类完成一些选择存储位置的操作，可复用
*/


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.scut.filemanager.FileManager;
import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.FileHandleFilter;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.controller.BaseController;
import com.scut.filemanager.ui.controller.TabDirectoryViewController;
import com.scut.filemanager.ui.protocols.LocationPickerCallback;
import com.scut.filemanager.ui.transaction.TransactionProxy;
import com.scut.filemanager.util.FMArrays;

import java.util.Comparator;
import java.util.List;

public class LocationPickDialogDelegate extends BaseController
        implements View.OnClickListener,    //监听视图点击事件
        AdapterView.OnItemClickListener, //监听listView
    com.scut.filemanager.ui.protocols.InputConfirmCallBack

{

    private FileHandle currentFileHandle=null;
    private Service svc=null;
    private AlertDialog alertDialog;
    //UI outlets
    private LinearLayout viewContainer;
    private TextView textView_location;
    private TextView textView_emptyTip;
    private ImageButton backBtn;
    private ImageButton mkdirBtn;
    private ListView listView;
    //data to be assembled
    private ArrayAdapter<FileHandle> arrayAdapter;
    //signal variables:
    private boolean notifyParentControllerUpdateContent=false;
    //handler and parentController
    private BaseController parentController;

    //other useful variables
    private FileHandle[] superFolder= TabDirectoryViewController.SuperFolder;

    //callback
    private LocationPickerCallback callback;

    //util
    private FileHandleFilter folderFilter=new FileHandleFilter() {
        @Override
        public boolean accept(FileHandle handle) {
            return handle.isDirectory();
        }
    };



    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //二次过滤目录
        if(!backBtn.isEnabled()){
            backBtn.setEnabled(true);
        }
        FileHandle nextFolder = arrayAdapter.getItem(i);
        currentFileHandle=nextFolder;
        FileHandle[] folders_in_next=nextFolder.listFiles(folderFilter);
        //send message to this Handler
        Message update_text_message=Message.obtain();
        update_text_message.what=InnerMessageCode.UPDATE_LOCATION_TEXT;
        update_text_message.obj=nextFolder.getAbsolutePathName();
        this.mHandler.sendMessage(update_text_message); //send it to handler

        setNewCollection(folders_in_next);      //update contents in adapter
    }




    /*
    @Example:
    @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.dialog_signin, null))
            // Add action buttons
                   .setPositiveButton(R.string.signin, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int id) {
                           // sign in the user ...
                       }
                   })
                   .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           LoginDialogFragment.this.getDialog().cancel();
                       }
                   });
            return builder.create();
        }
     */
    /*
        @Description:传入参数包括上一次的位置，父控制器（主要用于获取布局上下文）以及回调接口
    */

    public LocationPickDialogDelegate(FileHandle lastFileHandle, @NonNull BaseController parentController, LocationPickerCallback callback) {
        super();
        this.callback = callback;
        this.parentController = parentController;
        svc = this.parentController.getFileManagerCoreService();


        if (svc != null) {
            if (lastFileHandle != null) {
                this.currentFileHandle = lastFileHandle;
            } else {
                this.currentFileHandle = svc.getStorageDirFileHandle();
            }
        }
        onCreateDialog();
    }

    public LocationPickDialogDelegate(Context context,@NonNull Service service,LocationPickerCallback callback){
        super();
        this.callback=callback;
        this.svc=service;
        this.currentFileHandle=this.svc.getStorageDirFileHandle();
        onCreateDialog();
    }

    public void onCreateDialog() {

        AlertDialog.Builder builder=new AlertDialog.Builder(svc.getContext());

        //set up dialog title and buttons
        builder.setTitle(R.string.dialog_locationPicker_title);

        builder.setPositiveButton(R.string.dialog_locationPicker_positiveBtnTitle, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                callback.onLocationPicked(currentFileHandle);
                //When the user touches any of the action buttons created with an AlertDialog.Builder,
                // the system dismisses the dialog for you. ?
                //dismiss();
            }
        });
        //negative button has been delegated ?
        builder.setNegativeButton(R.string.dialog_locationPicker_negativeBtnTitle, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                callback.onLocationPickerDialogCancel(currentFileHandle,notifyParentControllerUpdateContent);
                dialogInterface.cancel();
            }
        });

        //set up dialog custom view ------------------------------------------------------------------------------

        viewContainer= (LinearLayout) this.svc.getContext().getLayoutInflater()
                .inflate(R.layout.dialog_select_location,null);
        builder.setView(viewContainer);
        listView=viewContainer.findViewById(R.id.listview_for_selecting_folders);

        //configure listener of items
        listView.setOnItemClickListener(this);


        //get outlets reference
        textView_location=viewContainer.findViewById(R.id.textview_dialog_display_location);
        textView_location.setText(currentFileHandle.getAbsolutePathName());
        textView_emptyTip=viewContainer.findViewById(R.id.textview_loc_pick_emptyFolder_tip);
        textView_emptyTip.setText(R.string.emptyFolderHint);
        backBtn=viewContainer.findViewById(R.id.imgbtn_location_picker_dialog_back);
        backBtn.setOnClickListener(this); //remind of setting its listener
        mkdirBtn=viewContainer.findViewById(R.id.imgbtn_mkdir);
        mkdirBtn.setOnClickListener(this);


        //setUp connection between adapter and listView
        FileHandle[] list_of_files=currentFileHandle.listFiles(folderFilter);
        if(list_of_files==null){
            list_of_files=new FileHandle[0]; //make sure the argument transferred into adapter isn't null!
            this.mHandler.sendMessage(
                    obtainMessage(InnerMessageCode.SHOW_EMPTY_FOLDER_TIP,0)
            );
        }

        //传入可变列表
        List<FileHandle> ls=FMArrays.asList(list_of_files);

        arrayAdapter= new ArrayAdapter<>(this.svc.getContext(),
                R.layout.dialog_loc_sel_listview_item,
                R.id.textview_item_name, ls
        );
        listView.setAdapter(arrayAdapter);
        this.sort(FileManager.Default_FileHandleComparator); //sort them

        //display title
        setDisplayLocation(currentFileHandle.getAbsolutePathName());
        //-----------------------------------------------------------------------------------------------------
        alertDialog=builder.create();

    }

    public void showDialog(){
        alertDialog.show();
    }

    private void setDisplayLocation(String location){
        Message msg=Message.obtain();
        msg.what=InnerMessageCode.UPDATE_LOCATION_TEXT;
        msg.obj=location;
        this.mHandler.sendMessage(msg);
    }



    @Override
    public Context getContext() {
        return svc.getContext();
    }

    public Handler getHandler() {
        return this.mHandler;
    }


    public Service getFileManagerCoreService() {
        return svc;
    }

    /*
        @Description:deal with back button clicked
        @Params: this view was clicked
    */

    @Override
    public void onClick(View view) {
        if(view.getId()== R.id.imgbtn_location_picker_dialog_back){ //返回键处理
            //same as return key down, return to parent folder
            if(currentFileHandle.isAndroidRoot()){ //display superFolders
                Message msg=Message.obtain();
                msg.what=InnerMessageCode.UPDATE_LOCATION_TEXT;
                msg.obj=this.getFileManagerCoreService().getContext().getResources().getString(R.string.superfolder_default_name);
                this.mHandler.sendMessage(msg);
                currentFileHandle=FileHandle.superHandle;
                backBtn.setEnabled(false);
                setNewCollection(superFolder);
                Log.d("back btn:", "onClick: back to super folder");
            }
            else{
                //return to parent folder normally
                currentFileHandle=currentFileHandle.getParentFileHandle();
                Message msg=Message.obtain();
                msg.what=InnerMessageCode.UPDATE_LOCATION_TEXT;
                msg.obj=currentFileHandle.getAbsolutePathName();
                this.mHandler.sendMessage(msg);
                setNewCollection(currentFileHandle.listFiles(folderFilter));
                Log.d("back btn:", "onClick: back to previous folder");
            }
        }
        else{                                                   //新建文件夹
            Log.d("locationPicker","newFolderBtn pressed");
            SingleLineInputDialogDelegate delegate=new SingleLineInputDialogDelegate(SingleLineInputDialogDelegate.DialogType.NEW_DIRECTORY,
                    this,this);
            delegate.showDialog();
        }
    }

    /*
        @Description:更新列表内的元素，通知UI刷新空文件夹提示
    */

    private void setNewCollection(FileHandle... fileHandles){
        arrayAdapter.clear();
        //过滤空元素,主要取决于列表是否支持空元素
        if(fileHandles!=null&&fileHandles.length>0) {
            arrayAdapter.addAll(fileHandles);
            this.sort(FileManager.Default_FileHandleComparator);
            this.mHandler.sendMessage(obtainMessage(InnerMessageCode.SHOW_EMPTY_FOLDER_TIP,1));
        }
        else {
            Log.d("setNewColletion", "show empty folder tip ");
            this.mHandler.sendMessage(obtainMessage(
                    InnerMessageCode.SHOW_EMPTY_FOLDER_TIP, 0
            ));
        }
    }

    private void addNewItem(@NonNull FileHandle newFolder){
        if(newFolder.isDirectory()){
            arrayAdapter.add(newFolder);
        }
    }

    public void sort(@NonNull Comparator<FileHandle> comparator){
        arrayAdapter.sort(comparator);
    }


    /*
        @Description: false when currentHandle cannot retrieve continuely
        @Params:
    */
    private boolean validateLocation(){
        return !FileHandle.superHandle.isDenotedToSameFile(currentFileHandle);
    }

    /*
        @Description:对话框的接口方法
    */

    @Override
    public void onInputConfirmClicked(String text, int action) {
        String fileName= text; //确保有效
        boolean mkdir_result=FileHandle.makeDirectory(currentFileHandle,fileName);
        if(mkdir_result){
            Toast.makeText(getContext(),R.string.ToastHint_mkdir_true,Toast.LENGTH_SHORT)
                    .show();

            //update adapter resource
            setNewCollection(currentFileHandle.listFiles(folderFilter));
        }
        else{
            Toast.makeText(getContext(),R.string.ToastHint_mkdir_false,Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private static class InnerMessageCode{
        static final int UPDATE_LOCATION_TEXT=10;
        static final int SHOW_EMPTY_FOLDER_TIP=11;
    }

    /*
        @Description:only the handler can call this method
    */

    private void adjustEmptyFolderTipsView(boolean empty){
        listView.setVisibility( empty?View.GONE:View.VISIBLE);
        textView_emptyTip.setVisibility(empty?View.VISIBLE:View.GONE);
    }

    private Message obtainMessage(int what,int agr1){
        Message msg=Message.obtain();
        msg.what=what;
        msg.arg1=agr1;
        return msg;
    }


    protected void setUpHandler(){
        this.mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what){
                    case InnerMessageCode.UPDATE_LOCATION_TEXT:
                        String loc_desc= (String) msg.obj;
                        if(loc_desc==null){
                            loc_desc=""; //safety convert
                        }
                        textView_location.setText(loc_desc);
                        break;
                    case InnerMessageCode.SHOW_EMPTY_FOLDER_TIP:
                        LocationPickDialogDelegate.this
                                .adjustEmptyFolderTipsView(
                                        msg.arg1==0?true:false //0为隐藏
                                );
                        Log.d("show list or tip", ""+(msg.arg1==0?true:false));
                        break;

                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        };
    }



    protected void setUpProxy(){
        this.proxy=new TransactionProxy(this){
            @Override
            public void sendRequest(Message message) {
                switch (message.what){
                    case SingleLineInputDialogDelegate.DialogType.NEW_DIRECTORY:
                        String fileName= (String) message.obj; //确保有效
                        boolean mkdir_result=FileHandle.makeDirectory(currentFileHandle,fileName);
                        if(mkdir_result){
                            Toast.makeText(getContext(),R.string.ToastHint_mkdir_true,Toast.LENGTH_SHORT)
                            .show();

                            //update adapter resource
                            setNewCollection(currentFileHandle.listFiles(folderFilter));
                        }
                        else{
                            Toast.makeText(getContext(),R.string.ToastHint_mkdir_false,Toast.LENGTH_SHORT)
                                    .show();
                        }

                        break;
                    default:
                        break;
                }
            }
        };
    }





}
