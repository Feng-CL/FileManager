package com.scut.filemanager.core.net;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Queue;

//元数据封装类
public class FileNodeWrapper {
    private FileNode tree=null;
    private String rootPath="";
    private long totalSize=0L;


    public FileNodeWrapper(FileNode root,boolean calculateSize){
        this.tree=root;
        if(calculateSize){
            this.calculateSize();
        }
    }

    public void calculateSize(){
        if(tree!=null){
            totalSize=tree.calculateSize();
        }
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public FileNode getFileNodeTree(){
        return tree;
    }

    //迭代应该在多台设备执行的结果一致,否则无法解析数据块
    /**
     * 每次调用此函数将会重置迭代进度。迭代按照广度优先算法进行
     *
     * 需要解决的问题是，如何保证每次迭代都是相同的顺序？
     * @return
     */
    public Iterator<FileNode> iterator(){
        Iterator<FileNode> _iterator=new Iterator<FileNode>() {

            FileNode current=FileNodeWrapper.this.getFileNodeTree();
            Queue<FileNode> queue;
            Iterator<FileNode> fileNodeIterator;//implemented by LinkedHashSet's iterator
            @Override
            public boolean hasNext() {
                return current!=null;
            }

            @Override
            public FileNode next() { //must be wrapped under closure of hasNext()
                FileNode next=current;
                if(fileNodeIterator==null){
                    if(current.hasChildren()){
                        fileNodeIterator=current.children.iterator();
                        if(fileNodeIterator.hasNext()){
                            current=fileNodeIterator.next();
                            queue.add(current);
                        }
                        else {
                            current=null;
                        }
                    }
                    else{
                        current=null;
                    }
                }
                else{
                    if(fileNodeIterator.hasNext()){
                        current=fileNodeIterator.next();
                        queue.add(current);
                    }
                    else if(!queue.isEmpty()){
                        current=queue.poll();
                        if(current.hasChildren()){
                            fileNodeIterator=current.children.iterator();
                        }
                    }
                    else {
                        current=null;
                    }
                }
                return next;
            }
        };
        return _iterator;
    }
}
