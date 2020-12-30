package com.scut.filemanager.core.net;

import com.scut.filemanager.core.FileHandle;

import java.util.List;

public class FolderNode {
    public List<FileHandle> childrens=null;
    public String name;

    public boolean hasChildren(){
        if(childrens!=null){
            return childrens.size()>0;
        }
        else {
            return false;
        }
    }

}
