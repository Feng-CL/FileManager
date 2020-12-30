package com.scut.filemanager;

import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.net.NetService;

import java.util.Comparator;

public class FMGlobal {
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

    static public final int MAKE_TOAST=-2; //special message code
}
