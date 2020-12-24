package com.scut.filemanager.ui;

import android.content.Context;
import android.os.Handler;
import android.view.ViewGroup;

public abstract class BaseController {
    protected Handler mHandler;
    protected ViewGroup parentView;
    protected BaseController parentController;

    abstract public Context getContext();
    abstract public Handler getHandler();

}
