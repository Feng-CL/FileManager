package com.scut.filemanager.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.TabViewController;
import com.scut.filemanager.ui.adapter.SimpleListViewItemAssembler;
import com.scut.filemanager.ui.protocols.DialogCallBack;
import com.scut.filemanager.ui.transaction.Request;
import com.scut.filemanager.util.FMFormatter;

//import java.util.logging.Handler;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.LogRecord;

public class NotifyDialog {


    private AlertDialog.Builder builder;
    private String titleText;
    private String positiveBtn;
    private String negativeBtn;
    private String messageText;

    //UI outlet
    private AlertDialog alertDialog;
    private TextView textView_detail;

    private DialogCallBack theCallBack;

    private Context context;
    private DialogInterface.OnClickListener onPositiveButtonClickListener;
    private DialogInterface.OnClickListener onNegativeButtonClickListener;

    private int actionType;

    //data source
    private List<FileHandle> listOfFiles;
    private FileHandle fileHandle;
    Timer refreshTimer=null;
    public Handler mHandler = new Handler(Looper.getMainLooper()){

        @Override
        public void handleMessage(@NonNull Message msg){
            switch (msg.what){
                case dialogMessageCode.REFRESH_TEXT:
                    textView_detail.setText((String) msg.obj);
                    break;
                case dialogMessageCode.REFRESH_STOP:
                    if(refreshTimer!=null){
                        refreshTimer.cancel();
                    }
                    break;
                case dialogMessageCode.CLOSE_DIALOG:{
                    alertDialog.dismiss();
                    theCallBack.onDialogClose(false);
                }

            }
        }




    };

    private static class dialogMessageCode{
        static private final int GET_DETAIL = 1;
       // static private final int SHOW_DIALOG = 2;
        static private final int REFRESH_STOP = 3;
        static private final int REFRESH_TEXT=4;
        static final int CLOSE_DIALOG=5;
    }

    public static class dialogType{
        static public final int ACTION_DELETE = 1;
        static public final int ACTION_DETAIL = 2;
        static public final int ACTION_DETAIL_MULTI=3;
    }

    public NotifyDialog(int action, Context context, DialogCallBack callBack){
        builder = new AlertDialog.Builder(context);
        actionType = action;
        theCallBack = callBack;
        this.context = context;
        positiveBtn = context.getResources().getString(R.string.dialog_positive_button_text);
        negativeBtn = context.getResources().getString(R.string.dialog_negative_button_text);


        setListener();
        setDialog();
        //callBack.onDialogCancel();
    }



    public void setDataSource(FileHandle fileHandle){
        this.fileHandle=fileHandle;
    }

    public void setDataSource(List<FileHandle> list){
        listOfFiles=list;
    }

    public void setMessageText(String msgText){
        messageText = msgText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;
    }

    public void showDialog(){
        setDetailMessage();
        alertDialog.show();
    }

    private void setDialog(){
        switch(actionType){
            case dialogType.ACTION_DELETE:
                titleText = context.getResources().getString(R.string.dialogTitle_delete);
                messageText = context.getResources().getString(R.string.delete_confirm);
                setDoubleButtonDialog();
                break;
            case dialogType.ACTION_DETAIL:
                titleText = context.getResources().getString(R.string.dialogtitle_detail);
                //这里获取不变的信息
                setSingleButtonDialog();
                break;
            case dialogType.ACTION_DETAIL_MULTI:
                titleText="Many Files";
                setSingleButtonDialog();
            default:
                break;
        }

    }


    private void setSingleButtonDialog(){
        textView_detail=new TextView(builder.getContext());
        textView_detail.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView_detail.setPadding(10,0,10,0);
        alertDialog= builder.setTitle(titleText)
                .setPositiveButton(positiveBtn, onPositiveButtonClickListener)
                .setNegativeButton(null,null)
                .setView(textView_detail)
                .create();
    }

    private void setDoubleButtonDialog(){
       alertDialog= builder.setTitle(titleText)
                .setPositiveButton(positiveBtn, onPositiveButtonClickListener)
                .setNegativeButton(negativeBtn, onNegativeButtonClickListener)
                .setMessage(messageText).create();
    }

    private void setListener(){
        onNegativeButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                theCallBack.onDialogCancel();
                if(refreshTimer!=null) {
                    refreshTimer.cancel();
                }
            }
        };
        onPositiveButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                theCallBack.onDialogOk();
                if(refreshTimer!=null) {
                    refreshTimer.cancel();
                }
            }
        };
    }

    private void setDetailMessage() {
        if (actionType != dialogType.ACTION_DELETE) {
            if (refreshTimer == null) {
                refreshTimer = new Timer(false);
            }
            if (actionType == dialogType.ACTION_DETAIL) {
                RefreshTask task = new RefreshTask(fileHandle.totalSize(), fileHandle.getFileCount());
                refreshTimer.scheduleAtFixedRate(task, 0, 1000);
            } else {
                RefreshTask task = new RefreshTask(listOfFiles);
                refreshTimer.scheduleAtFixedRate(task, 0, 1000);
            }

        }
    }

    class RefreshTask extends TimerTask {

        Future<Long> future_result;
        List<FileHandle> listFiles=null;
        private int cursor=1;
        private long accumulatedSize=0L;
        private String name="name: ",path="path: ",size="size: ",count="",modifiedTime="";
        private boolean isList=false;
        StringBuilder text=new StringBuilder();

        public RefreshTask(Future<Long> future,int count){
            future_result=future;
            name=name.concat(fileHandle.getName());
            path=path.concat(fileHandle.getAbsolutePathName());
            modifiedTime=modifiedTime.concat(FMFormatter.timeDescriptionConvert_simpleLongToString(fileHandle.getLastModifiedTime()));
            this.count="count: "+count;
        }

        public RefreshTask(List<FileHandle> listOfFiles){
            listFiles=listOfFiles;
            path="path: "+listOfFiles.get(0).getAbsolutePathName();
            count="count: "+listFiles.size();
            name=name+"multiple files";
            isList=true;
            future_result=listOfFiles.get(0).totalSize();
        }

        @Override
        public void run() {
            text.setLength(0);
            if(isList){

                text.append(name).append('\n').append(path).append('\n');
                long current_get_size=0L;
                boolean plus=false;
                    try {
                        current_get_size=future_result.get(); //首次获取
                    } catch (ExecutionException | InterruptedException e) {return;}

                    if(future_result.isDone()&& cursor<listFiles.size()) {
                        try {
                            current_get_size=future_result.get(); //完成后再获取
                        } catch (ExecutionException | InterruptedException e) {return;}
                        future_result = listFiles.get(cursor).totalSize();
                        plus=true;
                        cursor++; //这将导致最后一个无法加上去
                    }
                    if(plus){
                        //Log.d("plus",""+current_get_size);
                        accumulatedSize+=current_get_size;
                        text.append(size+FMFormatter.getSuitableFileSizeString(accumulatedSize)).append('\n').append(count);
                    }
                    else{
                        text.append(size+FMFormatter.getSuitableFileSizeString(accumulatedSize+current_get_size)).append('\n').append(count);
                    }
            }
            else{
                text.append(name).append('\n').append(path).append('\n');
                    try {
                        text.append(size+FMFormatter.getSuitableFileSizeString(future_result.get())).append('\n').append(count);
                    } catch (ExecutionException e) {
                        future_result.cancel(true);
                        return;
                    } catch (InterruptedException e) {
                        future_result.cancel(true);
                        return;
                    }
            }
            mHandler.sendMessage(
                    Request.obtain(dialogMessageCode.REFRESH_TEXT,text.toString())
            );
        }
    }



}
