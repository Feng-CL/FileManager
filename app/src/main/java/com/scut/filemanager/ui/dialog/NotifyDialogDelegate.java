package com.scut.filemanager.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import androidx.annotation.NonNull;

import com.scut.filemanager.R;
import com.scut.filemanager.ui.protocols.DialogCallBack;
import com.scut.filemanager.ui.transaction.Request;


/**
 * 该代理将会默认创建一个包含两个按钮的通知对话框，使用alertDialog的默认布局
 * 如果需要设置其他属性，需要调用该代理类的其他方法。
 */
public class NotifyDialogDelegate {

    private DialogCallBack callBack;
    private Context context;
    private AlertDialog dialog;
    private Handler mHandler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case UIMessageCode.CHANGE_MESSAGE:
                    dialog.setMessage((String)msg.obj);
                    break;
                case UIMessageCode.CHANGE_TITLE:
                    dialog.setTitle((String)msg.obj);
                    break;
                case UIMessageCode.CHANGE_VIEW:
                    dialog.setView((View)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    public NotifyDialogDelegate(Context context, final DialogCallBack callBack,String title,String notification){
        this.callBack=callBack;
        this.context=context;

        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        title=title==null?"":title;
        notification=notification==null?"":notification;
        builder.setTitle(title);
        builder.setMessage(notification);
        builder.setPositiveButton(R.string.dialog_positive_button_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callBack.onDialogOk(dialog);
            }
        });
        builder.setNegativeButton(R.string.dialog_negative_button_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callBack.onDialogCancel(dialog);
            }
        });


        dialog= builder.create();

    }

    public void setTitle(String title){
        mHandler.sendMessage(
                Request.obtain(UIMessageCode.CHANGE_TITLE,title )
        );
    }


    public void setMessage(String msg){
        mHandler.sendMessage(
                Request.obtain(UIMessageCode.CHANGE_MESSAGE,msg)
        );
    }

    public void setView(View view){
        mHandler.sendMessage(
                Request.obtain(UIMessageCode.CHANGE_VIEW,view)
        );
    }

    public void show(){
        dialog.show();
    }

    static final class UIMessageCode{
        static final int CHANGE_TITLE=1;
        static final int CHANGE_MESSAGE=2;
        static final int CHANGE_VIEW=3;
        static final int CHANGE_ICON=4;
    }
}
