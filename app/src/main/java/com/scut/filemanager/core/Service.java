package com.scut.filemanager.core;


import java.io.File;
import java.util.logging.FileHandler;


public class Service {

    Service(){};

    //返回根目录绝对路径名
    public String getRootDirPathName(){
        File root=android.os.Environment.getRootDirectory();
        return root.getAbsolutePath();
    }

    //返回封装类对象的句柄
    public FileHandle getRootDirFileHandle(){
        File root=android.os.Environment.getRootDirectory();
        return new FileHandle(root);
    }

    //由文件路径名
}
