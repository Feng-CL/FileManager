package com.scut.filemanager.ui.dialog;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.controller.BaseController;
import com.scut.filemanager.ui.protocols.InputConfirmCallBack;
import com.scut.filemanager.ui.protocols.SingleLineInputDialogCallBack;

/*
    @Description:一个可复用的单行输入代理控制器，在创建时，只需要为其指定类型，
    在做数据传递时，通过传入的parentController.getProxy().sendRequest(Message)
    按照预定义的协议传递数据
*/

public class SingleLineInputDialogDelegate extends BaseController{

    //UI　Outlets
    private EditText editText;
    private AlertDialog dialog;

    //define dialog type
    private int type;
    //define callback object
    private InputConfirmCallBack callBack;

    /*
        @Description:为创建对话指定类型，类型有两种，定义在OutterMessageCode中
    */
    public SingleLineInputDialogDelegate(int TYPE, BaseController parentController, final InputConfirmCallBack callBack){
        this.callBack=callBack;
        this.parentController=parentController;
        type=TYPE;
        String dialogTitle="";
        switch (TYPE){
            case DialogType.NEW_DIRECTORY:
                dialogTitle=getContext().getResources().getString(R.string.dialogTitle_newFolder);
                break;
            case DialogType.NEW_FILE:
                dialogTitle=getContext().getResources().getString(R.string.dialogTitle_newFile);
                break;
            case DialogType.RENAME:
                dialogTitle=parentController.getContext().getResources().getString(R.string.dialogTitle_rename);
                break;
            default:
                //shouldn't happen !
                type= DialogType.ERROR;
                dialogTitle="error";
                break;
        }
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());


        DialogInterface.OnClickListener listener=new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: { //确认按钮

                        if(editText!=null) {
                            //判断editText  中的内容
                            switch (type){
                                //三种情况相同的处理流程
                                case DialogType.NEW_DIRECTORY: case DialogType.NEW_FILE: case DialogType.RENAME:{
                                    String fileName=editText.getText().toString();
                                    if(fileName.isEmpty()||FileHandle.containsIllegalChar(fileName)){
                                        //alert user
                                        Toast.makeText(getContext(),R.string.editText_illegal_input_hint,Toast.LENGTH_SHORT)
                                                .show(); //this is the temporary alert code,consider to replace it in the next version
                                    }

                                    else {
                                        //重命名操作需要检验合理行,这里需要做提示操作,同时需要为对话框重新写onDismiss 操作
                                        callBack.onInputConfirmClicked(fileName,type );
                                    }
                                }
                                break;
                                default: //deal with other case
                                    break;

                            }
                        }
                        else{
                            dialogInterface.cancel();
                        }
                    }
                    break;
                    case DialogInterface.BUTTON_NEGATIVE:{ //取消按钮
                        if(callBack instanceof SingleLineInputDialogCallBack){
                            ((SingleLineInputDialogCallBack) callBack).onInputDialogCancelClick(editText.getText().toString());
                        }
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

        switch (TYPE){
            case DialogType.NEW_DIRECTORY:
                editText.setHint(R.string.editText_placeholder_mkdir);
                break;
            case DialogType.NEW_FILE:
                editText.setHint(R.string.editText_placeholder_newFile);
                break;
            default:
                editText.setHint(R.string.editText_placeholder);
                break;
        }
        builder.setView(editText);

        dialog=builder.create();

    }

    public void showDialog(){
        if(dialog!=null){
            dialog.show();
        }
    }

    public void setEditTextInitialContent(String content){
        editText.setText(content);
    }

    public void setSelectedTextAndCallInputMethod(int start, int end){
        //Log.d("InputDialog", "setSelectedTextAndCallInputMethod: "+editText.isFocused());
        editText.setSelection(start,end);
        //Log.d("InputDialog", "setSelectedTextAndCallInputMethod: "+editText.isFocused());
        //editText.requestFocus();
//        InputMethodManager inputMethodManager= (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        if(inputMethodManager!=null){
//            Log.d("InputDialog", "setSelectedTextAndCallInputMethod: "+inputMethodManager.showSoftInput(editText,InputMethodManager.SHOW_FORCED));
//        }
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


    static public class DialogType {
        static public final int NEW_FILE=1;
        static public final int NEW_DIRECTORY=2;
        static public final int RENAME=4;
        static public final int ERROR=3;
    }
}
