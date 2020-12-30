package com.scut.filemanager.ui;

import android.content.Context;
import android.os.Handler;
import android.view.ViewGroup;

import com.scut.filemanager.FMGlobal;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.transaction.TransactionProxy;

public abstract class BaseController {
    protected Handler mHandler;
    protected ViewGroup parentView;
    protected BaseController parentController;
    protected TransactionProxy proxy;

    public BaseController(){
        setUpHandler();
        setUpProxy();
    }
    abstract public Context getContext();
    abstract public Handler getHandler();
    abstract public Service getFileManagerCoreService();
    public TransactionProxy getProxy(){
        return proxy;
    }
    protected void setUpHandler(){}    //empty stub
    protected void setUpProxy(){} //empty stub
    public  void makeToast(String text){
        if(mHandler!=null){
            mHandler.sendEmptyMessage(FMGlobal.MAKE_TOAST);
        }
    }

}
