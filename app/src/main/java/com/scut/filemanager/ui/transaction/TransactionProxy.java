package com.scut.filemanager.ui.transaction;

import android.os.Message;

import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.BaseController;

/*
主要用于区分更新UI和事务操作的区别
代理视图控制器做一些后台操作，并通知前台更新视图，具体代理方式由
对应控制器（Director)决定
 */
public class TransactionProxy {

    private BaseController director;



    public TransactionProxy(BaseController ProxyController){
        director=ProxyController;
    }


    public void sendRequest(Message message){

    }

}
