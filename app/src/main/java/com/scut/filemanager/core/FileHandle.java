package com.scut.filemanager.core;
import android.icu.text.DecimalFormat;
import android.icu.text.NumberFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.lang.Math;
import java.nio.file.NoSuchFileException;
import java.text.FieldPosition;




@RequiresApi(api = Build.VERSION_CODES.O)
public class FileHandle {
    //文件句柄类，处理与文件各种交互问题，包括元数据的编辑，查看etc
    //基于file的自定义接口类。
    //基本文件属性判断，RWX
    //文件的ctime, mtime,atime
    //文件的大小
    //文件的其他数据段，元数据：
    //

    File file; //not nullable
    String CanonicalPathName=""; //包含文件名在内的正则路径名,一般使用这个
    String AbsolutePathName="";     // 包含文件名在内的绝对路径名


    enum FileAccessPosition{
        Owner,Group,User,CurrentID
        //CurrentID描述当前用户的身份，就是指应用本身。
    };

    public FileHandle(File f) throws IOException,NullPointerException {
        if(f==null)
            throw new NullPointerException("[FileHandle:Null pointer initialization]");
        file=f.getCanonicalFile();
    }

    public FileHandle(String pathname) throws IOException {
        file=new File(pathname);
        CanonicalPathName=file.getCanonicalPath();
        AbsolutePathName=file.getAbsolutePath();
        file=file.getCanonicalFile();
    }

    public File getFile(){
        return file;
    }

    public String getName(){
        return file.getName();
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
    //注意，这个Size仅仅返回文件本身的大小,如果该文件是目录，则返回目录这个i-node的大小而不包括目录项
    //下的具体内容
    public long Size(){
        return file.length();
    }

    //返回该文件总大小，如果是目录则计量目录下的文件大小
    //该方法的实现需要确保线程安全
    public long totalSize() throws IOException {
        long total=Size();
        FileHandle[] list_item=listFiles();
        if(list_item==null){
            return total;
        }
        else{
            for(int i=0;i<list_item.length;i++){
                total+=list_item[i].totalSize();
            }
            return total;
        }
    };



    //获取文件当前所在路径绝对名,如/home/dir/file.dat,使用此方法获得/home/dir/
    public String getCanonicalPathName() throws IOException {
        return file.getCanonicalPath();
    }

    //获取文件所在的目录名，父目录名(包含路径前缀),
    public String getParentName() throws NullPointerException, IOException {
        File parent=file.getParentFile();
        if(parent==null){
            return null;
        }
        return parent.getCanonicalPath();
    }

    //如果不存在ParentFile则返回空
    public FileHandle getParentFileHandle() throws IOException {
        File parent=file.getParentFile();
        if(parent==null){
            return null;
        }
        return new FileHandle(parent);
    }

    public int getFileCount() throws IOException {
        if(isDirectory()){
            FileHandle[] listFiles=listFiles();
            if(listFiles==null){
                return 0;
            }
            else{
                return listFiles.length;
            }
        }
        else{
            return 0;
        }
    }


    public boolean isRoot(){
        return (file.getParentFile()==null);
    }

    //FileHandle封装下的可空文件句柄判断
    @Deprecated
    public boolean isNull(){
        return (file==null);
    }

    //重命名，删除, 属于敏感操作

    /*
    修改文件名需要使用合法的字符和长度，为了效率，合法检测交由前端处理。
     */
    synchronized boolean rename(String newName) throws IOException {
        String NewFileName=getParentName();
        NewFileName=NewFileName.concat("/"+newName);
        return file.renameTo(new File(NewFileName));
    }

    //删除文件，如果这是非空文件夹，则会返回false
    //目标文件不存在或无法删除都将返回false,可能会抛出异常

    synchronized boolean delete() throws IOException{
        //delete方法的实现受API限制

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            try{
                Files.delete(file.toPath());
                return true;
            }
            catch(NoSuchFileException ex){
                android.util.Log.e("[FileHandle delete]","no such file exist");
                throw ex; //向上抛出以通知无法删除的原因。
            }
            catch(DirectoryNotEmptyException ex){
                android.util.Log.e("[FileHandle delete]","Directory isn't empty");
                throw ex;
            }
            catch(SecurityException ex){
                android.util.Log.e("[FileHandle delete]","file is under protected by security manager");
                throw ex;
            }
        }
        else{
            try {
                return file.delete();
            }
            catch(SecurityException ex){
                android.util.Log.e("[FileHandle delete]","file is under protected by security manager");
                throw ex;
            }
        }

    }

    //如果该句柄是文件，则返回空，否则返回目录下的项目句柄s。
    //获取目录下的项目句柄
    public FileHandle[] listFiles() throws IOException {
        if(file.isDirectory()){
            File[] list=file.listFiles();
            if(list==null){
                return null; //空目录
            }
            else{
                int item_number=list.length;
                FileHandle[] listFileHandle=new FileHandle[item_number];
                for(int i=0;i<item_number;i++){
                    listFileHandle[i]=new FileHandle(list[i]);
                }
                return listFileHandle;
            }
        }
        else{
            return null;
        }
    }
    //元数据metadata 的操作

    public long getLastModifiedTime(){
        return file.lastModified();
    }

    /*
    平台差异，暂时不考虑ctime, atime
     */
//    public long getCreateTime(){
//        return
//    }

    //Private methods


    //-----------------static methods---------------------------




    //---------------分割线--------------------------------


    //private helper function---------------------------------


}
