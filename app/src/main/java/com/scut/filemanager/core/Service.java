package com.scut.filemanager.core;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.logging.FileHandler;

//单线程服务对象，使用单体模式
public class Service {

    public enum Service_CopyOption{
        REPLACE_EXISTING,   //替换已存在
        COPY_ATTRIBUTE,    //不修改mtime
        NOT_FOLLOWINGLINK, //不跟随符号链接

    }

    Service(Context app_context){
        //initialize member object
        if(app_context==null){
            Log.e("core.Service","can not initialize without context");
            throw new NullPointerException("[core.Service]: argument 'app_context' is not nullable");
        }

        context=app_context;
        String ExternalStorageState=android.os.Environment.getExternalStorageState();
        if(ExternalStorageState.equals(
                Environment.MEDIA_MOUNTED
        )){
            storage_emulated_0=android.os.Environment.getExternalStorageDirectory();
        }
        else if(ExternalStorageState.equals(
                Environment.MEDIA_MOUNTED_READ_ONLY
        )){
            status=SERVICE_STATUS.READ_ONLY;
        }
        else {
            //这种情况其实是忽略了外部存储的其他可能状态，因为外部存储介质可能正在检查，抑或是处于未知状态
            //开发前期先将这些情况都归类到异常情况。
            status=SERVICE_STATUS.EXCEPTION;
        }


        if(storage_emulated_0==null){
            //shouldn't happen
            throw new NullPointerException("[core.Service]:getExternalStorageDirectory failed");
        }
        if(storage_emulated_0.listFiles()==null){
            status=SERVICE_STATUS.UNABLE_TO_READ_FILE_LIST;
        }
        else if (storage_emulated_0.canWrite()){
            status=SERVICE_STATUS.OK;
        }
        else if(storage_emulated_0.canRead()){
            status=SERVICE_STATUS.READ_ONLY;
        }
        else{
            status=SERVICE_STATUS.EXCEPTION;
            Log.d("core.Service","exception status");
        }


        //no need to check out these variable because of the previous check include the case of the below one
        app_private_external_dir=context.getExternalFilesDir(null);
        app_private_internal_dir=context.getFilesDir();

        //a tricky way to get the handle of SD_CARD, but need api > 19
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.KITKAT) {
            File[] package_zone_dir = context.getExternalFilesDirs(null);
            StringBuilder sdcard_dir_str;
            if(package_zone_dir.length==2&&package_zone_dir[1]!=null){
                sdcard_dir_str=new StringBuilder(package_zone_dir[1].getAbsolutePath());
                int cut_pos=sdcard_dir_str.indexOf("/Android");
                sdcard_dir_str.delete(cut_pos,sdcard_dir_str.length());
                storage_sdcard=new File(sdcard_dir_str.toString());
                sdcard_status=SERVICE_STATUS.SDCARD_MOUNTED;
            }
            else{
                sdcard_status=SERVICE_STATUS.SDCARD_UNKNOWN; //sd卡未知错误
                storage_sdcard=null;
            }
        }
        else{
            sdcard_status=SERVICE_STATUS.SDCARD_UNMOUNTED; //sd卡因api等级限制暂时无法获取。
            storage_sdcard=null;
        }

    };

    public enum SERVICE_STATUS{
        OK,                     //正常情况
        READ_ONLY,              //检测到只读
        SDCARD_UNMOUNTED, SDCARD_MOUNTED,SDCARD_UNKNOWN,SDCARD_EXCEPTION,
        UNABLE_TO_READ_FILE_LIST,   //检测到不可读取文件列表
        EXCEPTION                   //意外情况，用于调试
    }

    public static Service getInstance(Context app_context){
        if (!(svc instanceof Service)) {
            svc = new Service(app_context);
        }
        return svc;
    }

    //app 启动前，应该去检查这些组件的状态。
    public static SERVICE_STATUS getStatus(){
        return status;
    }


    //注意，这里的根目录指代/storage/emulated/0
    //返回根目录绝对路径名
    //API 级别 R

    public String getRootDirPathName() {
        return storage_emulated_0.getAbsolutePath();
    }

    //返回封装类对象的句柄
    public FileHandle getRootDirFileHandle() throws IOException {
        return new FileHandle(storage_emulated_0);
    }

    public String getInternalPrivateDirectoryPathName(){
        return app_private_internal_dir.getAbsolutePath();
    }

    public String getExternalPrivateDirectoryPathName(){
        return app_private_external_dir.getAbsolutePath();
    }

    //返回SD卡根目录名称
    public String getSDCardRootDirectoryPathName(){
        if(sdcard_status==SERVICE_STATUS.SDCARD_MOUNTED){
            return storage_sdcard.getAbsolutePath();
        }
        else{
            return null;
        }
    }

    //返回SD卡根目录句柄, 如果状态不合法则返回null
    public FileHandle getSDCardRootDirectoryFileHandle() throws IOException {
        if(sdcard_status==SERVICE_STATUS.SDCARD_MOUNTED){
            return new FileHandle(storage_sdcard);
        }
        else{
            return null;
        }
    }

    //返回结果为 /storage/emulated/0/pathname
    public  String getPathUnderRootDir(String pathname){
        String temp=storage_emulated_0.getAbsolutePath()+"/";
        return temp.concat(pathname);
    }

    public  FileHandle getFileHandleUnderRootDir(String pathname)throws IOException{
        String path=getPathUnderRootDir(pathname);
        return new FileHandle(path);
    }

    public String getPathUnderSdCard(String pathname){
        String temp=storage_sdcard.getAbsolutePath()+"/";
        return temp.concat(pathname);
    }

    //参数src,dst需要指明是根目录还是sd卡根目录，函数会先判断目录有效性后再采取复制策略。
    public boolean copyTo(FileHandle src, FileHandle dst,Service_CopyOption... options){
        int arg_len=options.length;
        if(parseOption(
            Service_CopyOption.REPLACE_EXISTING,options
        )){
            //
        }
        return false;
    }


    //.-------------------------------------------------

    private static Service svc;
    private static File storage_emulated_0=null;
    private static File storage_sdcard=null; //here is a tricky way to get the handle of it

    //Returns the absolute path to the directory on the primary shared/external storage device
    //where the application can place persistent files it owns. These files are internal to the applications,
    //and not typically visible to the user as media.
    private static File app_private_external_dir=null;
    private static File app_private_internal_dir=null;
    private static SERVICE_STATUS status;
    private static SERVICE_STATUS sdcard_status;
    private static android.content.Context context;


    private boolean copyByStream(){
        //
        return false;
    }

    private boolean copyBySystemDependService(){
        return false;
    }

    private static <T> boolean parseOption(T e, T[] elements){
       T iter;
       for(int i=0;i<elements.length;i++){
           iter=elements[i];
            if(iter.equals(e)){
                return true;
            }
       }
       return false;
    }

}
