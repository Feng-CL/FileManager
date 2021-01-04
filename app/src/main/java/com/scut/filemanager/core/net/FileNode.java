package com.scut.filemanager.core.net;

import com.scut.filemanager.core.FileHandle;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileNode {
    public Set<FileNode> children =null; //之所以只是使用Set是因为文件名具有唯一性，不可以让列表中出现两个文件
    public String name;
    public long size=0L;

    public FileNode(boolean isDirectory){
        if(isDirectory){
            children=new HashSet<>();
        }
    }

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
        for (FileNode node :
                children) {
            size += node.size;
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
    public static FileNode createNodeFromList(String name, long size,List<FileHandle> fileHandles){
        FileNode node;
        if(fileHandles==null){
            node=new FileNode(false);
            node.size=0L;
        }
        else{
            node=new FileNode(true);
            for (FileHandle h :
                    fileHandles) {
                node.children.add(createNodeFromArray(h.getName(),h.Size(), h.listFiles()));
            }
            node.size=size;
        }
        node.name=name;
        return node;
    }


    public static FileNode createNodeFromArray(String name, long size,FileHandle[] handleArray){
        FileNode node;
        if(handleArray==null){
            node=new FileNode(false);
            node.size=0L;
        }
        else{
            node=new FileNode(true);
            for (FileHandle h :
                    handleArray) {
                node.children.add(createNodeFromArray(h.getName(), h.Size(),h.listFiles()));
            }
            node.size=size;
        }
        node.name=name;
        return node;
    }


}
