package com.scut.filemanager.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.scut.filemanager.R;
import com.scut.filemanager.ui.TabViewController;
import com.scut.filemanager.ui.adapter.SimpleListViewItemAssembler;
import com.scut.filemanager.ui.protocols.DialogCallBack;
import com.scut.filemanager.util.FMFormatter;

//import java.util.logging.Handler;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.logging.LogRecord;

public class NotifyDialog {


    private AlertDialog.Builder builder;
    private String titleText;
    private String positiveBtn;
    private String negativeBtn;
    private String messageText;

    private DialogCallBack theCallBack;

    private Context context;
    private DialogInterface.OnClickListener onPositiveButtonClickListener;
    private DialogInterface.OnClickListener onNegativeButtonClickListener;
    private SimpleListViewItemAssembler adapter;

    private int actionType;

    public Handler mHandler = new Handler(Looper.getMainLooper()){
        Timer refreshTimer=null;
        @Override
        public void handleMessage(@NonNull Message msg){
            switch (msg.what){
                case dialogMessageCode.GET_DETAIL:
                    //builder.create().show();
                    if(refreshTimer==null){
                        refreshTimer = new Timer(true);
                    }
                    mTimerTask task = new mTimerTask();
                    refreshTimer.scheduleAtFixedRate(task,500,500);
                    break;
                case dialogMessageCode.SHOW_DIALOG:
                    builder.show();
                    break;
                case dialogMessageCode.REFRESH_STOP:
                    if(refreshTimer!=null){
                        refreshTimer.cancel();
                    }
                    break;
            }
        }

        class mTimerTask extends TimerTask {

            String name = adapter.getSelectedFile().getName();
            String size = "";
            String contentCount = "";
            String modifyTime = FMFormatter.timeDescriptionConvert_simpleLongToString(adapter.getSelectedFile().getLastModifiedTime());
            String path = adapter.getSelectedFile().getAbsolutePathName();

            @Override
            public void run() {
                if (!adapter.getSelectedFile().totalSize().isDone()||!adapter.getSelectedFile().getFileTotalCount().isDone()){
                    try {
                        size = adapter.getSelectedFile().totalSize().get().toString();
                        contentCount = adapter.getSelectedFile().getFileTotalCount().get().toString();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    messageText = name+"\n"+size+"\n"+contentCount+"\n"+modifyTime+"\n"+path;
                    setMessageText(messageText);
                    Log.d("messageText",messageText);
                    setDialog();
                    mHandler.sendEmptyMessage(dialogMessageCode.SHOW_DIALOG);
                }else {
                    mHandler.sendEmptyMessage(dialogMessageCode.REFRESH_STOP);
                }

            }
        }


    };

    private static class dialogMessageCode{
        static private final int GET_DETAIL = 1;
        static private final int SHOW_DIALOG = 2;
        static private final int REFRESH_STOP = 3;
    }

    public static class dialogType{
        static public final int ACTION_DELETE = 1;
        static public final int ACTION_DETAIL = 2;
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
        callBack.onDialogCancel();
    }

    public void loadDetailMessage(SimpleListViewItemAssembler adapter){
        this.adapter = adapter;
        setDetailMessage();
    }

    public void setMessageText(String msgText){
        messageText = msgText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;
    }

    public void showDialog(){
        builder.create().show();
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
                    setSingleButtonDialog();
                break;
            default:
                break;
        }

    }

    private void setSingleButtonDialog(){
        builder.setTitle(titleText)
                .setPositiveButton(positiveBtn, onPositiveButtonClickListener)
                .setNegativeButton(null,null)
                .setMessage(messageText);
    }

    private void setDoubleButtonDialog(){
        builder.setTitle(titleText)
                .setPositiveButton(positiveBtn, onPositiveButtonClickListener)
                .setNegativeButton(negativeBtn, onNegativeButtonClickListener)
                .setMessage(messageText);
    }

    private void setListener(){
        onNegativeButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                theCallBack.onDialogCancel();
            }
        };
        onPositiveButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                theCallBack.onDialogOk();
            }
        };
    }

    private void setDetailMessage(){
        mHandler.sendEmptyMessage(dialogMessageCode.GET_DETAIL);
    }








}
