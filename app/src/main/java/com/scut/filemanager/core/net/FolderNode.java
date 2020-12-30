package com.scut.filemanager.core.net;

import com.scut.filemanager.core.FileHandle;

import java.util.List;

public class FolderNode {
    public List<FileHandle> children =null;
    public String name;

    public boolean hasChildren(){
        if(children !=null){
            return children.size()>0;
        }
        else {
            return false;
        }
    }


}
