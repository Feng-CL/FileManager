package com.scut.filemanager.core;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.scut.filemanager.util.SimpleArrayFilter;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;

import java.io.IOException;
import java.nio.file.Files;



//单线程服务对象，使用单体模式
public class Service {

    public enum Service_CopyOption{
        REPLACE_EXISTING,   //替换已存在
        COPY_ATTRIBUTE,    //不修改mtime
        NOT_FOLLOWINGLINK, //不跟随符号链接
        RECURSIVE_COPY //递归复制文件夹
    }

    public Service(Context app_context){
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
            Log.d("core.Service","Get external storage directory successfully");
        }
        else if(ExternalStorageState.equals(
                Environment.MEDIA_MOUNTED_READ_ONLY
        )){
            status=SERVICE_STATUS.READ_ONLY;
            Log.d("core.Service","External storage state is read only now");
        }
        else {
            //这种情况其实是忽略了外部存储的其他可能状态，因为外部存储介质可能正在检查，抑或是处于未知状态
            //开发前期先将这些情况都归类到异常情况。
            status=SERVICE_STATUS.EXCEPTION;
            Log.d("core.Service","status is unknown and encounter an unknown exception");
        }


        if(storage_emulated_0==null){
            Log.e("core.Service","cannot get the file handle of storage_emulated_0");
            //shouldn't happen
            throw new NullPointerException("[core.Service]:getExternalStorageDirectory failed");

        }
        if(storage_emulated_0.listFiles()==null){
            status=SERVICE_STATUS.UNABLE_TO_READ_FILE_LIST;
            Log.d("core.Service","unable to read file under /storage/emulated/0");
        }
        else if (storage_emulated_0.canWrite()){
            status=SERVICE_STATUS.OK;
            Log.d("core.Service","status is ok");
        }
        else if(storage_emulated_0.canRead()){
            status=SERVICE_STATUS.READ_ONLY;
            Log.d("core.Service","status is read only");
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
            Log.i("core.Service","Build version api > 19(KITKAT)");
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
            Log.i("[core.Service:]","sdcard cannot be got limited by API-Level and now sdcard status is unmounted status");
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

    public Context getContext(){
        return context;
    }


    //app 启动前，应该去检查这些组件的状态。
    public SERVICE_STATUS getStatus(){
        return status;
    }
    public SERVICE_STATUS getSdcardStatus(){
        return sdcard_status;
    }

    //注意，这里的根目录指代/storage/emulated/0
    //返回根目录绝对路径名
    //API 级别 R

    /*
    * 返回根目录（即外部存储中的內部存储路径名*/
    public String getRootDirPathName() {
        return storage_emulated_0.getAbsolutePath();
    }

    //返回封装类对象的句柄
    public FileHandle getRootDirFileHandle(){
        return new FileHandle(storage_emulated_0);
    }

    /*
    * 这里的internal 和external 都是相对于内部存储卡和sd卡而言的
    * 在根据下的/data /system  我们暂时不需要管理*/
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
    public FileHandle getSDCardRootDirectoryFileHandle() {
        if(sdcard_status==SERVICE_STATUS.SDCARD_MOUNTED){
            return new FileHandle(storage_sdcard);
        }
        else{
            return null;
        }
    }

    /*
    @Description:方便用于快速获取一个在内存存储卡下的路径，注意输入路径pathname
    前不需要以 “/” 开头
    @Params: pathname:String
    @Return:返回结果为 /storage/emulated/0/pathname
     */
    public  String getPathUnderRootDir(String pathname){
        String temp=storage_emulated_0.getAbsolutePath()+"/";
        return temp.concat(pathname);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public  FileHandle getFileHandleUnderRootDir(String pathname){
        String path=getPathUnderRootDir(pathname);
        return new FileHandle(path);
    }

    //返回结果为 /storage/SD_CARD/pathname
    public String getPathUnderSdCard(String pathname){
        String temp=storage_sdcard.getAbsolutePath()+"/";
        return temp.concat(pathname);
    }

    //
    public Thread copy(FileHandle src, String dstPath,ProgressMonitor monitor,Service_CopyOption... options){
        int arg_len=options.length;
        boolean replace_existing,copy_attribute,not_following_link,recursive_copy;
        replace_existing=parseOption(Service_CopyOption.REPLACE_EXISTING,options);
        copy_attribute=parseOption(Service_CopyOption.COPY_ATTRIBUTE,options);
        not_following_link=parseOption(Service_CopyOption.NOT_FOLLOWINGLINK,options);
        recursive_copy=parseOption(Service_CopyOption.RECURSIVE_COPY,options);

        //testCode-------------------------------------------------------
        FileHandle dst=new FileHandle(dstPath);
        if(src.isExist()&&dst.isExist()){
            CopyTask copyTask=new CopyTask(0,monitor,src,dst);
            Thread copyThread=new Thread(copyTask);
            copyThread.start();
            return copyThread;
        }
        return null;
        //---------------------------------------------------------------
    }

    /*
    @Description: 判断给定路径是否存在文件，如果路径不合法，则返回false
     */
    public  boolean ExistsAtPath(String pathname){
        if(isLegalPath(pathname)){
            FileHandle target=new FileHandle(pathname);
            return target.isExist();
        }
        else {
            return false;
        }
    }


    /*
    @Description:判断给定路径是否合法,其中合法情况为：
    1.目标存在于：
    /storage/emulated/0
    /storage/sd_card/
    2.同时目标可访问(accessible)
     */
    public boolean isLegalPath(String pathname){
        String storage_0_path_prefix=getRootDirPathName();
        String storage_sdcard_path_prefix=getSDCardRootDirectoryPathName();
        if(storage_sdcard_path_prefix==null){
            sdcard_status=SERVICE_STATUS.SDCARD_UNMOUNTED;
        }

        if(pathname.startsWith(storage_0_path_prefix)&&(!pathname.endsWith("/"))){
            return true;
        }
        else if(pathname.startsWith(storage_sdcard_path_prefix)&&(!pathname.endsWith("/"))){
            return true&&(sdcard_status==SERVICE_STATUS.SDCARD_MOUNTED);
        }
        else{
            return false;
        }
    }

    /*
    @Description: 获取sdcard的型号,当然如果sdcard未挂载，返回null
     */
    public String getSDCardModel(){
        StringBuilder sdcard_model_strBuilder=new StringBuilder(getSDCardRootDirectoryPathName());
        if(sdcard_model_strBuilder.length()!=0){
            int cut_pos=sdcard_model_strBuilder.lastIndexOf("/");
            return sdcard_model_strBuilder.substring(cut_pos+1,sdcard_model_strBuilder.length());
        }
        return null;
    }

    /*
    @Description: 服务级别的判断一个给定路径是否在sdcard 下，如果sd卡未挂载，则会返回false
     */
    public boolean isUnderSDCardPath(String pathname){
        String sdcard_str=getSDCardRootDirectoryPathName();
        if(sdcard_str!=null){
            return pathname.startsWith(sdcard_str);
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


    /*
    @Description： 其实每次复制都要考虑目标目录所在卷的可用空间大小，否则
    复制将会因为空间不足而失败，同时，参考的可用空间也是变化的，可能会出现
    在复制的过程中出现因为空间不足而抛出异常的极限情况。这些将会作为一个测
    试点来进行考虑。
     */
    protected boolean copyByStream(FileHandle src_handle,FileHandle dst_handle,ProgressMonitor<String,Long> monitor,int taskId)  {

        //assume both handles are effect, and do the copy work

        long sizeInBytes=src_handle.Size();
        long numberOfBytesCopied=0;

        monitor.onSubTaskStart(taskId);

        try {
            //need to make sure this operation is atomic
            //如果目标是目录，则不能通过流复制
            if(src_handle.isDirectory()){
                if(!dst_handle.isExist()) { //optimize this process
                    boolean makeDirResult = dst_handle.makeDirectory();
                    FileInputStream fileInput=new FileInputStream(dst_handle.file);
                    return makeDirResult;
                }
                else{
                    return true; //dst_handle exists
                }
            }
            else {

                FileInputStream fileInput = new FileInputStream(src_handle.file);
                FileOutputStream fileOutput = new FileOutputStream(dst_handle.file);
                BufferedInputStream bufferInput = new BufferedInputStream(fileInput);
                BufferedOutputStream bufferOutput = new BufferedOutputStream(fileOutput);

                //4k buffer
                int blockSize = 4*1024 * 1024;
                byte[] buffer = new byte[blockSize];
                int lastBlockLength = (int) (sizeInBytes % blockSize);
                long numberOfBlocks = sizeInBytes / blockSize + 1;  //this number contains tail block, used to align blocks
                long numberOfBlocksCopied = 0;

                while (numberOfBlocksCopied < numberOfBlocks - 1) {
                    try {
                        bufferInput.read(buffer);
                        bufferOutput.write(buffer);

                        numberOfBlocksCopied++;
                        numberOfBytesCopied += blockSize;

                        monitor.onSubProgress(taskId, "numberOfBytesCopied", numberOfBytesCopied);

                    } catch (IOException e) {
                        //record and report to monitor
                        Log.e("core.Service", "copying file by stream has incurred an IOException" + " Exception message: " +
                                e.getMessage());
                        monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED);
                        return false;
                    }
                }

                //deal with tail
                int tailLength = (int) (sizeInBytes % blockSize);
                if (tailLength != 0) {
                    try {
                        bufferInput.read(buffer, 0, tailLength);
                        bufferOutput.write(buffer, 0, tailLength);

                        numberOfBlocksCopied++;
                        numberOfBytesCopied += blockSize;

                        monitor.onSubProgress(taskId, "numberOfBytesCopied", numberOfBytesCopied);
                    } catch (IOException e) {
                        Log.e("core.Service", "copying file by stream has incurred an IOException" + " Exception message: " +
                                e.getMessage());
                        monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED);
                        return false;
                    }
                }

                fileInput.close();
                fileOutput.close();
                monitor.onSubTaskFinish(taskId);
                return true;
            }
        }
        catch(FileNotFoundException ex){
            Log.e("core.Service","File not found or some wrong happen");
            monitor.onSubTaskStop(taskId,ProgressMonitor.PROGRESS_STATUS.FAILED);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    /*
    @Description: 使用系统的服务进行复制，目前实现有点绕
     */
    private boolean copyBySystemDependService(){
        return false;
    }

    /*
    复制之前先通告monitor 任务id和任务详情，再进行复制，如果复制失败则此子任务id最终的状态
    回以Fail 结束
     */
    /*
    @Description:另外这里的监视器协议receiveMessage所接受的code代表子任务id,每个子任务都会相应地调用
                 进度监视器的监视周期函数。
    @Params: 1. taskId: 传入当前完成的最后一个的任务id
            2. src_folder: 传入源文件夹的文件句柄
            3. dst: 传入目标文件夹的文件句柄，注意不是复制以后的名字，这仅仅是个父目录的文件句柄
            4. monitor: 进度监视器
    @Return:  返回这次递归结束后的任务id
     */
    //返回该递归任务完成后的任务id,输入当前的任务id
    private int copyByStreamRecursively(int taskId,FileHandle src_folder,FileHandle dst,ProgressMonitor<String,Long> monitor){
       //复制文件夹这里用宽度优先算法
        //assert src_folder and dst both are effective
        FileHandle[] list=src_folder.listFiles();

        taskId++;

        //need to copy the folder first
        FileHandle dst_handle=new FileHandle(dst,"/"+src_folder.getName());
        FileHandle iterator_handle=dst_handle.clone();
        monitor.receiveMessage(taskId,src_folder.getAbsolutePathName()+"->"+dst_handle.getAbsolutePathName());
        copyByStream(src_folder,dst_handle,monitor,taskId); //create src_folder here, src_folder can either be a file


        if(list!=null){

            //breath first
            for(int i=0;i<list.length;i++){
                taskId++;
                monitor.receiveMessage(taskId,list[i].getAbsolutePathName());
                iterator_handle.pointTo(dst_handle,"/"+list[i].getName());
                copyByStream(list[i],iterator_handle,monitor,taskId);
            }

            //filter the list and get folders
            SimpleArrayFilter<FileHandle> simpleArrayFilter=new SimpleArrayFilter<>();
            FileHandle[] folders;
            Object[] rawArray=simpleArrayFilter.filter(list, new FileHandleFilter() {
                @Override
                public boolean accept(FileHandle handle) {
                    return handle.isDirectory();
                }
            });
            folders=new FileHandle[rawArray.length];
            for(int i=0;i<rawArray.length;i++){
                folders[i]=(FileHandle)rawArray[i];
            }


            //copy continues recursively
            for(int i=0;i<folders.length;i++){
                //dst_handle.pointTo();
                taskId=copyByStreamRecursively(taskId,folders[i],dst_handle,monitor);
            }
        }
        return taskId;

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

    public class CopyTask implements Runnable{

        protected  ProgressMonitor<String,Long> monitor;
        protected FileHandle src,dst;
        int pid;

        public CopyTask(int initial_taskId,ProgressMonitor<String,Long> arg_monitor,FileHandle arg_src,FileHandle arg_dst){
            pid=initial_taskId;
            monitor=arg_monitor;
            dst=arg_dst;
            src=arg_src;
        }

        @Override
        public void run() {
            monitor.onStart();
            copyByStreamRecursively(pid,src,dst,monitor);
            monitor.onFinished();
        }
    }
}
