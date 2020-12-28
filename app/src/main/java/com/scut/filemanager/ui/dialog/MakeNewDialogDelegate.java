package com.scut.filemanager.ui.dialog;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.BaseController;
import com.scut.filemanager.ui.transaction.Request;

public class MakeNewDialogDelegate extends BaseController{

    //UI　Outlets
    private EditText editText;
    private AlertDialog dialog;

    //define dialog type
    private int type;

    /*
        @Description:为创建对话指定类型，类型有两种，定义在OutterMessageCode中
    */
    public MakeNewDialogDelegate(int TYPE, BaseController parentController){

        this.parentController=parentController;
        type=TYPE;
        String dialogTitle="";
        switch (TYPE){
            case OutterMessageCode.NEW_DIRECTORY:
                dialogTitle=getContext().getResources().getString(R.string.dialogTitle_newFolder);
                break;
            case OutterMessageCode.NEW_FILE:
                dialogTitle=getContext().getResources().getString(R.string.dialogTitle_newFile);
                break;
            default:
                //shouldn't happen !
                type=OutterMessageCode.ERROR;
                dialogTitle="error";
                break;
        }
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());


        DialogInterface.OnClickListener listener=new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {

                        //判断editText  中的内容
                        if(editText!=null) {
                            String fileName=editText.getText().toString();
                            if(fileName.isEmpty()||FileHandle.containsIllegalChar(fileName)){
                                //alert user
                                Toast.makeText(getContext(),R.string.editText_illegal_input_hint,Toast.LENGTH_SHORT)
                                        .show();
                            }
                            else {
                                MakeNewDialogDelegate.this.parentController.getProxy()
                                        .sendRequest(Request.obtain(
                                                type, fileName //通过Message发送结果给控制器代理 [typeIndicator// ,newFileName]
                                        ));
                               
                            }
                        }
                        else{
                            dialogInterface.cancel();
                        }
                    }
                    break;
                    case DialogInterface.BUTTON_NEGATIVE:{
                        dialogInterface.cancel();
                    }
                    default:
                        break;
                }
            }
        };

        builder.setTitle(dialogTitle)
                .setPositiveButton(R.string.dialog_positive_button_text,listener)
                .setNegativeButton(R.string.dialog_negative_button_text,listener);


        //add OnTextChangeListener 限制输入的内容
        editText=new EditText(getContext());
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT
        ));
        editText.setHint(R.string.editText_placeholder);
        builder.setView(editText);

        dialog=builder.create();
    }

    public void showDialog(){
        if(dialog!=null){
            dialog.show();
        }
    }

    @Override
    public Context getContext() {
        return getFileManagerCoreService().getContext();
    }

    @Override
    public Handler getHandler() {
        return this.mHandler;
    }

    @Override
    public Service getFileManagerCoreService() {
        return parentController.getFileManagerCoreService();
    }


    static public class OutterMessageCode{
        static public final int NEW_FILE=1;
        static public final int NEW_DIRECTORY=2;
        static public final int ERROR=3;
    }
}
