package com.scut.filemanager.core.net;

import androidx.annotation.NonNull;

import com.scut.filemanager.core.FileHandle;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FileNode implements Serializable {
    public LinkedHashSet<FileNode> children =null; //之所以只是使用Set是因为文件名具有唯一性，不可以让列表中出现两个文件
    public String name;
    public FileNode parent=null;
    public long size=0L;

    public FileNode(boolean isDirectory){
        if(isDirectory){
            children=new LinkedHashSet<>();
        }
    }


    /**
     * true value will implicitly indicate that it is a folder
     * else it is a file or an directory
     * @return
     */
    public boolean hasChildren(){
        if(children !=null){
            return children.size()>0;
        }
        else {
            return false;
        }
    }

    /**
     * 计算节点所包含的文件的总大小，这里不包括文件夹的大小
     * @return
     */
    public long calculateSize(){
        long size=this.size;
        for (FileNode child :
                children) {
            size += child.calculateSize();
        }
        return size;
    }

    /**
     * 当该节点孩子为空是，确定该节点为文件,否则为文件夹
     * @return boolean
     */
    public boolean isDirectory(){
        return children!=null;
    }

    public boolean isFile(){
        return children==null;
    }

    /**
     * Notice: 创建节点时，不应该把文件夹大小也算上，这是需要注意的。
     * @param name 节点名字
     * @param size  节点大小
     * @param fileHandles 子节点数据来源
     * @return FileNode 文件节点
     */
    public static FileNode createNodeFromList(String name, long size,List<FileHandle> fileHandles,FileNode parent){
        FileNode node;
        if(fileHandles==null){
            node=new FileNode(false);
        }
        else{
            node=new FileNode(true);
            for (FileHandle h :
                    fileHandles) {
                node.children.add(createNodeFromArray(h.getName(),h.Size(), h.listFiles(),node ));
            }
        }
        node.name=name;
        node.size=size;
        node.parent=parent;
        return node;
    }


    public static FileNode createNodeFromArray(String name, long size,FileHandle[] handleArray,FileNode parent){
        FileNode node;
        if(handleArray==null){
            node=new FileNode(false);
        }
        else{
            node=new FileNode(true);
            for (FileHandle h :
                    handleArray) {
                node.children.add(createNodeFromArray(h.getName(), h.Size(),h.listFiles(),node));
            }
        }
        node.size=size;
        node.parent=parent;
        node.name=name;
        return node;
    }

    public String getPath(){
        if(parent==null){
            return "";
        }
        else{
            return parent.getPath().concat("/").concat(name);
        }
    }

    public FileHandle toFileHandle(String attachToRoot){
        return new FileHandle(
                attachToRoot+getPath()
        );
    }


}
