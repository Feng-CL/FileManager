package com.scut.filemanager.ui.transaction;


/*
    @Description:类似Message的请求类，方便生成请求
*/

import android.os.Message;

public class Request {

    public static Message obtain(int what, int arg1,int arg2,Object object){
        Message msg=Message.obtain();
        msg.what=what;
        msg.arg1=arg1;
        msg.arg2=arg2;
        msg.obj=object;
        return msg;
    }

    public static Message obtain(int what,Object object){
        Message msg=Message.obtain();
        msg.what=what;
        msg.obj=object;
        return msg;
    }

    public static Message obtain(int what,int arg1,Object object){
        Message msg=Message.obtain();
        msg.what=what;
        msg.arg1=arg1;
        msg.obj=object;
        return msg;
    }

}
