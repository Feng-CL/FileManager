package com.scut.filemanager.ui.transaction;


/*
    @Description:类似Message的请求类，方便生成请求
*/

import android.os.Message;

public class MessageBuilder {

    public static Message obtain(int what,Object obj){
        Message msg=Message.obtain();
        msg.what=what;
        msg.obj=obj;
        return msg;
    }

    public static Message obtain(int what, String string){
        Message message=Message.obtain();
        message.what=what;
        message.obj=string;
        return message;
    }

    public static Message obtain(int what){
        Message message=Message.obtain();
        message.what=what;
        return message;
    }

    public static Message obtain(int what, int arg1){
        Message message=Message.obtain();
        message.what=what;
        message.arg1=arg1;
        return message;
    }

    public static Message obtain(int what,int arg1,Object obj){
        Message message=Message.obtain();
        message.obj=obj;
        message.what=what;
        message.arg1=arg1;
        return message;
    }

    public static Message obtain(int what,int arg1,int arg2,Object obj){
        Message message=Message.obtain();
        message.obj=obj;
        message.what=what;
        message.arg1=arg1;
        message.arg2=arg2;
        return message;
    }
}
