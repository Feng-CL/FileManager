package com.scut.filemanager.core;
import android.icu.text.DecimalFormat;
import android.icu.text.NumberFormat;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.lang.Math;
import java.text.FieldPosition;




public class FileHandle {
    //文件句柄类，处理与文件各种交互问题，包括元数据的编辑，查看etc
    //基于file的自定义接口类。
    //基本文件属性判断，RWX
    //文件的ctime, mtime,atime
    //文件的大小
    //文件的其他数据段，元数据：
    //

    File file; //nullable



    enum FileAccessPosition{
        Owner,Group,User,CurrentID
        //CurrentID描述当前用户的身份，就是指应用本身。
    };

    FileHandle(File f){
        file=f;
    }
    FileHandle(String absolute_name){

    }

    public File getFile(){
        return file;
    }

    //判断文件属性
    //文件属性包括 4 个基本字段， 元数据
    //drwxr-xr-x
    public boolean isDirectory(){
        return file.isDirectory();
    }

    public boolean isFile(){
        return file.isFile();
    }

    public boolean isHidden(){
        return file.isHidden();
    }

    //文件名是否是隐藏文件的表达名
    public boolean isHiddenMarkOnFileName(){
        String fileName=file.getName();
        return fileName.startsWith(".");
    }

    //读写性
    public boolean canRead(FileAccessPosition enumFlag){
        switch(enumFlag){
            default:
                return file.canRead();
            case Owner:
                //code
                return false;

            case Group:
                //handle
                return false;

            case User:
                //deal with User
                return false;

        }
    }

    public boolean canWrite(FileAccessPosition enumFlag){
        switch(enumFlag){
            default:
                return file.canWrite();
            case Owner:
                //code
                return false;

            case Group:
                //handle
                return false;

            case User:
                //deal with User
                return false;

        }
    }

    public boolean canExecute(FileAccessPosition enumFlag){
        switch(enumFlag){
            default:
                return file.canExecute();
            case Owner:
                //code
                return false;

            case Group:
                //handle
                return false;

            case User:
                //deal with User
                return false;

        }
    }

    //文件大小，单位B，可使用FileHandle类的方法转换成其他单位。
    //注意，这个Size仅仅返回文件本身的大小
    public long Size(){
        return file.length();
    }

    //返回该文件总大小，如果是目录则计量目录下的文件大小
    //该方法的实现需要确保线程安全
    public long totalSize(){

    }

    //默认保留一位小数
    public static String longToString(String unit,FileHandle f,int savePoint){
        //
        long size=f.Size();
        double size_d;
        DecimalFormat decimalFormat=new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(savePoint);
        StringBuffer varStr=new StringBuffer();
        varStr.setLength(0);
        switch(unit){
            case "MB":
                size_d=(double)size/Math.pow(1024,2);
                varStr.append(decimalFormat.format(size_d));
                break;
            case "KB":
                size_d=(double)size/(double)1024;
                varStr.append(decimalFormat.format(size_d));
                break;
            case "GB":
                size_d=(double)size/Math.pow(1024,3);
                varStr.append((decimalFormat.format(size_d)));
                break;
            default:
                varStr.append(Long.toString(size));
                break;
        }

        return varStr.toString();
    }


    //获取文件当前所在路径绝对名,如/home/dir/file.dat,使用此方法获得/home/dir/
    public String getCanonicalPathName() throws IOException {
        return file.getCanonicalPath();
    }

    //获取文件所在的目录名，父目录名(不包含路径前缀)
    public String getParentName() throws NullPointerException{
        if(isNull()){
            throw new NullPointerException("[FileHandleException:NullPointer] this fileHandle is null");
        }
        File parent=file.getParentFile();
        return parent.getName();
    }

    //如果不存在ParentFile则返回空
    public FileHandle getParentFileHandle(){
        if(isNull()){
            throw new NullPointerException("[FileHandleException:NullPointer] this fileHandle is null");
        }
        File parent=file.getParentFile();
        return new FileHandle(parent);
    }

    public String getParent(){
        if(isNull()){
            throw new NullPointerException("[FileHandleException:NullPointer] this fileHandle is null");
        }
    }


    public boolean isRoot(){
        return (file.getParentFile()==null);
    }

    //FileHandle封装下的可空文件句柄判断
    public boolean isNull(){
        return (file==null);
    }

    //元数据metadata 的操作
}
