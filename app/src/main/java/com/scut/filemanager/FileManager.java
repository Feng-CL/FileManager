package com.scut.filemanager;

import android.app.Application;

import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.net.NetService;

import java.util.Comparator;

public class FileManager extends Application {
    static public int Default_shortAnimTime;
    static public int Default_longAnimTime;
    static public NetService netService=null;
    static public Comparator<FileHandle> Default_FileHandleComparator=new Comparator<FileHandle>() {
        @Override
        public int compare(FileHandle f1, FileHandle f2) {
            boolean isDir1=f1.isDirectory();
            boolean isDir2=f2.isDirectory();
            if(isDir1&&isDir2){//均为目录
                return f1.getName().compareTo(f2.getName());
            }
            else if(isDir1){ //只有1 是目录
                return 1;
            }
            else if(isDir2){ //2是目录
                return -1;
            }
            else{ //neither
                return f1.getName().compareTo(f2.getName());
            }
        }
    };

    static public int ListenerPort=33721;
    static public int BoardCastReceivePort=33720;
    static public final int Default_BlockSize=2*1024*1024;
    static public final int START_MAIN_DELAY=1500;
    static public final int MAKE_TOAST=-2; //special message code


    private static FileManager app;

    public static FileManager getInstance(){
        return app;
    }

    public boolean showHiddenFile=false;

    @Override
    public void onCreate() {
        super.onCreate();
        app=this;
    }


    //non-static member in app
}
