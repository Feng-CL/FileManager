package com.scut.filemanager.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.scut.filemanager.R;
import com.scut.filemanager.ui.protocols.ProgressDialogContentProvider;
import com.scut.filemanager.ui.transaction.Request;


public class ProgressDialogDelegate {

    private ProgressDialogContentProvider provider;
    private int action;
    private Activity context;
    //UI outlets
    AlertDialog alertDialog;
    TextView textView_task_desc,textView_progress_desc,textView_speed_desc,textView_timeTicking;
    ProgressBar progressBar;

    //UI handler
    Handler mHandler=new Handler(Looper.getMainLooper()){
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case UIMessageCode.UPDATE_PROGRESS_BAR:
                    String desc= (String) msg.obj;
                    progressBar.setProgress(msg.arg1,true);
                    textView_progress_desc.setText(desc);
                    break;
                case UIMessageCode.UPDATE_TASK_DESC:
                    textView_task_desc.setText((String)msg.obj);
                    break;
                case UIMessageCode.UPDATE_SPEED_DESC:
                    textView_speed_desc.setText((String)msg.obj);
                    break;
                case UIMessageCode.POP_NOTIFY_DIALOG:
                    AlertDialog.Builder builder=new AlertDialog.Builder(context);
                    String[] message= (String[]) msg.obj;
                    if(message==null){
                        message=new String[]{"error","unknown error"};
                    }
                    builder.setTitle(message[0]).setMessage(message[1]);
                    builder.create().show();
                    break;
                case UIMessageCode.CLOSE_DIALOG:
                    alertDialog.dismiss();
                    provider.onDialogClose(null,true);
                    break;
                case UIMessageCode.UPDATE_TITLE:
                    alertDialog.setTitle((String) msg.obj);
                    break;
                case UIMessageCode.UPDATE_TIME_TICKING:
                    textView_timeTicking.setText((String)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    public ProgressDialogDelegate(Activity context, final ProgressDialogContentProvider provider, int action){
        this.provider=provider;
        this.action=action;
        this.context=context;
        String Title="";
        switch (action){
            case ACTION.COPY:
                Title=context.getResources().getString(R.string.DialogProgress_copy);
                break;
            case ACTION.DELETE:
                Title=context.getResources().getString(R.string.DialogProgress_delete);
                break;
            case ACTION.SEND:
                Title=context.getResources().getString(R.string.DialogProgress_sending);
                break;
            case ACTION.MOVE:
                Title=context.getResources().getString(R.string.DialogProgress_move);
                break;
            case ACTION.RECEIVE:
                Title=context.getResources().getString(R.string.DialogProgress_receiving);
                break;
            default:
                break;
        }
        //build dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(Title);
        LinearLayout linearLayout= (LinearLayout) context.getLayoutInflater().inflate(R.layout.dialog_progress,null,false);
        builder.setView(linearLayout);
        //set up Outlet
        textView_progress_desc=linearLayout.findViewById(R.id.textview_progress_desc);
        textView_task_desc=linearLayout.findViewById(R.id.textview_progress_task_desc);
        textView_speed_desc=linearLayout.findViewById(R.id.textview_progress_speed_desc);
        textView_timeTicking=linearLayout.findViewById(R.id.textview_progress_time_ticking);

        progressBar=linearLayout.findViewById(R.id.progressbar_h);
        progressBar.setMax(provider.getMaxMeasure());
        progressBar.setProgress(0);


        //set up Listener and button
        builder.setPositiveButton("hide", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                provider.onDialogHide(dialogInterface);
            }
        }).setNegativeButton(R.string.dialog_negative_button_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                provider.onDialogCancel(dialogInterface);
            }
        }).setNeutralButton("pause", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                provider.onDialogNeutralClicked(dialogInterface);
            }
        });

        alertDialog=builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                final Button pauseBtn=alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                View.OnClickListener pauseBtnListener=new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(provider.isPause()){
                            pauseBtn.setText("pause");
                            provider.onDialogNeutralClicked(dialogInterface);
                        }
                        else {
                            pauseBtn.setText("resume");
                            provider.onDialogNeutralClicked(dialogInterface);
                        }
                    }
                };
                pauseBtn.setOnClickListener(pauseBtnListener);
            }
        });



    }

    public void showDialog(){
        alertDialog.show();
    }


    /**
     * 考虑到可以动态调整title内容，ACTION类其实是一个多余的操作。
     */
    public static final class ACTION{
        public static final int COPY=0;
        public static final int MOVE=1;
        public static final int SEND=2;
        public static final int DELETE=3;
        public static final int RECEIVE=4;
    }

    public static final class UIMessageCode{
        public static final int UPDATE_TASK_DESC=1;
        public static final int UPDATE_PROGRESS_BAR=2; //包含对进度描述的更新，arg1 为进度值,obj为描述
        public static final int UPDATE_SPEED_DESC=3;
        public static final int POP_NOTIFY_DIALOG=4; //obj 为String[] 0 为title 1为信息
        public static final int CLOSE_DIALOG=5;
        public static final int UPDATE_TITLE=6;
        public static final int UPDATE_TIME_TICKING=7;
    }

    public void update_progress_bar(int progress_value,String desc){
        this.mHandler.sendMessage(
                Request.obtain(UIMessageCode.UPDATE_PROGRESS_BAR,progress_value,desc)
        );
    }

    public void update_task_description(String task_desc){
        this.mHandler.sendMessage(
                Request.obtain(UIMessageCode.UPDATE_TASK_DESC,task_desc)
        );
    }

    public void update_speed_description(String speed_desc){
        this.mHandler.sendMessage(
                Request.obtain(UIMessageCode.UPDATE_SPEED_DESC,speed_desc)
        );
    }

    public void update_title(String title){
        this.mHandler.sendMessage(
                Request.obtain(UIMessageCode.UPDATE_TITLE,title )
        );
    }

    public void update_time_ticking(String time){
        this.mHandler.sendMessage(
                Request.obtain(UIMessageCode.UPDATE_TIME_TICKING,"Elapsed time: "+time)
        );
    }

    public void pop_notify_dialog(String title,String message){
        String[] tokens=new String[2];
        tokens[0]=title;    tokens[1]=message;
        this.mHandler.sendMessage(
                Request.obtain(UIMessageCode.POP_NOTIFY_DIALOG,tokens)
        );
    }

    public void closeDialog(){
        this.mHandler.sendEmptyMessage(
                UIMessageCode.CLOSE_DIALOG
        );
    }

    /**
     * 旧时API方法，现在不建议使用
     * @return
     */
    public Handler getHandler(){
        return mHandler;
    }
}
