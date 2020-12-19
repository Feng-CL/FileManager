package com.scut.filemanager.core;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.scut.filemanager.util.SimpleArrayFilter;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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

        //configure FileHandle necessary environments
        FileHandle._initPrefixName(this);

        //create thread executor
        /*
        here we use the default executorFactory for the moment;
         */
        if(svc_executor==null) {
            svc_executor = Executors.newCachedThreadPool();
        }
        Log.d("core.Service","create a executor for core.service");
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
    * 返回外部存储的根目录（即外部存储中的內部存储路径名*/
    public String getStorageDirPathName() {
        return storage_emulated_0.getAbsolutePath();
    }

    //返回封装类对象的句柄
    public FileHandle getStorageDirFileHandle(){
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

    /*
    @Description: 复制文件或文件夹到指定位置，需要提供目标路径和源文件，以及监视器和复制选项
    @Notices 请确保目标的有效性，dstPath必须是存在的路径，且是一个目录。
    @Parameters:
        src: 源文件句柄，可以是单个文件或者一个文件夹，当然传入的目标如果是不存在则该任务不会被执行。
        dstPath: 目标路径，如果目标路径同样需要经过检验，需要是可写且存在的路径
        monitor: 监视器对象，键值对类型为： <String,Long> 分别对应文件名和大小(字节)
        allowRollBack: 用于设置当丢弃复制操作时是否进行抹去已复制内容的操作
        options: Service_CopyOption枚举类型，目前只有RECURSIVE_COPY 和REPLACE_EXISTING可被解析
    @Protocol:
        任务开始后，先统计需要复制的文件的总大小totalSize，并以onProgress(K,V)函数，返回"totalSize":srcSize
        到进度监视器中。
        子任务开始时调用onSubTaskStart(), 并通过describeTask 设置子任务的title
        在复制过程中，receiveMessage和onStop()都是可选路径，即不一定会被调用。
        如果目标文件夹所在分区容量不足，同样会调用receiveMessage和onStop()通报监视器
        生命周期函数调用概要:
        onStart()->空间余量检测()
        [{
        onSubTaskStart(taskid)->describeTask(taskid,title)->
        [onSubProgress(FileAbsolutePathName,numberOfBytesCopied)*->]
        [
        receiveMessage(MsgCode,message)->
        onSubTaskStop(PROGRESS_STATUS.FAILED)
        ->]
        onSubTaskFinish(taskid)->
        }*]
        ->[receiveMessage(MsgCode,message)->onStop(PROGRESS_STATUS.FAILED)->]
        onFinish()

    abortSignal() 与abortSignal(int slot)
    默认丢弃使用abortSignal()检测，同时使用1号终止信号槽决定回滚是否应该终止

    信息码描述：
        receiveMessage(int code,String msg):
        0=找不到文件
        1=目标路径不可写
        2=目标路径不存在
        3=源文件不可读
        4=流关闭错误
        5=读写错误
        6=创建文件夹失败
        7=空间不足
        8=未知错误
        9=InterruptedException

     */
    public void copy(FileHandle src, String dstPath, ProgressMonitor<String,Long> monitor, boolean allowRollBack,Service_CopyOption... options){
        int arg_len=options.length;
        boolean replace_existing,copy_attribute,not_following_link,recursive_copy;
        replace_existing=parseOption(Service_CopyOption.REPLACE_EXISTING,options);
        copy_attribute=parseOption(Service_CopyOption.COPY_ATTRIBUTE,options);
        not_following_link=parseOption(Service_CopyOption.NOT_FOLLOWINGLINK,options);
        recursive_copy=parseOption(Service_CopyOption.RECURSIVE_COPY,options);

        //testCode-------------------------------------------------------
        FileHandle dst=new FileHandle(dstPath);
        if(!dst.isDirectory()){
            return; //exit point
        }

        monitor.onStart(); //onStart() 标志任务已被接受，比起在工作线程中执行onStart,可以有效防止工作线程未开始，任务因特殊原因就结束了
        if(src.isExist()&&dst.isExist()){
            CopyTask copyTask=new CopyTask(0,monitor,src,dst,allowRollBack);
            svc_executor.execute(copyTask);
        }
        else if(!src.isExist()){
            monitor.receiveMessage(3,"source file doesn't exist");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            monitor.onFinished();
        }
        else{
            monitor.receiveMessage(2,"Target directory doesn't exist");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            monitor.onFinished();
        }


        //---------------------------------------------------------------
    }

    /*
    @Description: 移动文件或文件夹到指定位置，需要提供目标路径和源文件，以及监视器
    @Notices: 需要注意的是，务必在进行此操作前检查目标对象的有效性
              如果设置了中断信号，需要先解除中断信号后再发送丢弃信号才可放弃该任务。
    @Params:
        src: 源文件句柄，可以是单个文件或者一个文件夹，当然传入的目标如果是不存在则该任务不会被执行。
        dstPath: 目标路径，如果目标路径同样需要经过检验，需要是可写且存在的路径
        monitor: 监视器对象，键值对类型为： <String,Long> 分别对应文件名和大小(字节)
    @Protocol:
        子任务开始时调用onSubTaskStart(), 并通过describeTask 设置子任务的title
        在复制过程中，receiveMessage和onStop()都是可选路径，即不一定会被调用。
        如果目标文件夹所在分区容量不足，同样会调用receiveMessage和onStop()通报监视器
        生命周期函数调用概要:
        onStart)->
        [{
        onSubTaskStart(taskid)->describeTask(taskid,title)->
        [onSubProgress(FileAbsolutePathName,numberOfBytesCopied)*->]
        [
        receiveMessage(MsgCode,message)->
        onSubTaskStop(PROGRESS_STATUS.FAILED)
        ->]
        onSubTaskFinish(taskid)->
        }*]
        ->[receiveMessage(MsgCode,message)->onStop(PROGRESS_STATUS.FAILED)->]
        onFinish()


    信息码描述：
        receiveMessage(int code,String msg):
        0=找不到文件
        1=目标路径不可写
        2=目标路径不存在
        3=源文件不可读

        //仅在不同挂载设备间复制时会出现
        4=流关闭错误
        5=读写错误
        6=创建文件夹失败
        //----------------------------

        7=空间不足
        8=未知错误
        9=InterruptedException

     */

    public void move(FileHandle src, String dstPath,ProgressMonitor<String,Long> monitor){
        //two type of dstPath

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
    /storage/$sd_card$/
    2.同时目标可访问(accessible)
    判断路径是否合法后应该加入非法字符的判断，这里没有对非法字符进行检测
    是否含有非法字符的检测见FileHandle类的静态方法containIllegalChar(String args)
     */
    public boolean isLegalPath(String pathname){
        String storage_0_path_prefix= getStorageDirPathName();
        String storage_sdcard_path_prefix=getSDCardRootDirectoryPathName();
        if(storage_sdcard_path_prefix==null){
            sdcard_status=SERVICE_STATUS.SDCARD_UNMOUNTED;
            return false;
        }

        if(pathname.startsWith(storage_0_path_prefix)&&(!pathname.endsWith("/"))){
            return true;
        }
        else if(pathname.startsWith(storage_sdcard_path_prefix)&&(!pathname.endsWith("/"))){
            return (sdcard_status==SERVICE_STATUS.SDCARD_MOUNTED);
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
    @Description: 以服务级别来判断一个给定路径是否在sdcard 下，如果sd卡未挂载，则会返回false
     */
    public boolean isUnderSDCardPath(@NonNull String pathname){
        String sdcard_str=getSDCardRootDirectoryPathName();
        return pathname.startsWith(sdcard_str);
    }

    public boolean isUnderStoragePath(@NonNull String pathname){
        String storage_pathname= getStorageDirPathName();

        return pathname.startsWith(storage_pathname);
    }

    /*
    @Description: 获取内置存储卡的存储空间的大小，单位为字节
    如果需要转换单位或增添转换所需的工具类，参见filemanager.utiL包中的方法
     */
    public long getStorageTotalCapacity(){
        return storage_emulated_0.getTotalSpace();
    }

    /*
    @Description: 获取
     */
    public long getStorageFreeCapacity(){
        return storage_emulated_0.getFreeSpace();
    }

    public long getStorageUsableCapacity(){
        return storage_emulated_0.getUsableSpace();
    }

    public long getSdcardTotalCapacity(){
        return storage_sdcard.getTotalSpace();
    }

    public long getSdcardFreeCapacity(){
        return storage_sdcard.getFreeSpace();
    }
    //private zone 比起私有方法，公有方法才是更应该关注的
    //-------------------------------------------------

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

    //concurrent
    private static ExecutorService svc_executor;


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

    private class CopyTask implements Runnable{

        protected  ProgressMonitor<String,Long> task_monitor;
        protected FileHandle src,dst;
        protected boolean needToRollBack=false;
        protected boolean allowRollBackFlag=true;
        protected boolean hasNotAborted=true; //prevent inconsistent

        int pid;

        public CopyTask(int initial_taskId,ProgressMonitor<String,Long> arg_monitor,FileHandle arg_src,FileHandle arg_dst,boolean allowRollBackFlag){
            pid=initial_taskId;
            task_monitor=arg_monitor;
            dst=arg_dst;
            src=arg_src;
            this.allowRollBackFlag=allowRollBackFlag;
        }

        @Override
        public void run() {
            //check whether destination has enough space before performing copying
            long srcSize=0L;
            try{
                srcSize=src._totalSize();
            }
            catch (IOException ioex){
                task_monitor.receiveMessage(3,ioex.getMessage());
                task_monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                task_monitor.onFinished();
                return;
            }
            long free_space=0L;
            if(dst.isInStorageVolume()){
                free_space=getStorageFreeCapacity();
            }else if(dst.isInSDCardVolume()){
                free_space=getSdcardFreeCapacity();
            }
            else{
                //should not happen
                task_monitor.receiveMessage(1,"destination path is unreachable");
                task_monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                task_monitor.onFinished();
                return;
            }
            if(free_space<srcSize){
                task_monitor.receiveMessage(7,"no enough space to copy files");
                task_monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                task_monitor.onFinished();
                return;
            }

            //working
            //report totalSize first
            task_monitor.onProgress("totalSize",srcSize);
            copyByStreamRecursively(pid,src,dst,task_monitor);

            if(needToRollBack && allowRollBackFlag && hasNotAborted){
                rollBack();
            }

            task_monitor.onFinished();
        }



        /*
        @Notice： 其实每次复制都要考虑目标目录所在卷的可用空间大小，否则
        复制将会因为空间不足而失败，同时，参考的可用空间也是变化的，可能会出现
        在复制的过程中出现因为空间不足而抛出异常的极限情况。这些将会作为一个测
        试点来进行考虑。
        */
        protected boolean copyByStream(FileHandle src_handle,FileHandle dst_handle,ProgressMonitor<String,Long> monitor,int taskId)  {

            //assume both handles are effect, and do the copy work
            if(!monitor.abortSignal()&&hasNotAborted) {

                long sizeInBytes = src_handle.Size();
                long numberOfBytesCopied = 0;

                monitor.onSubTaskStart(taskId);
                monitor.describeTask(taskId, src_handle.getAbsolutePathName()); //notify monitor what this taskId is mapped to
                try {
                    //need to make sure this operation is atomic
                    //如果目标是目录，则不能通过流复制
                    if (src_handle.isDirectory()) {
                        if (!dst_handle.isExist()) { //optimize this process
                            boolean makeDirResult = dst_handle.makeDirectory();
                            if (!makeDirResult) {
                                monitor.receiveMessage(6, "creating directory fails");
                                monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED); //createDirectory fails
                            }
                            monitor.onSubTaskFinish(taskId);
                            return makeDirResult;
                        } else {
                            monitor.onSubTaskFinish(taskId);
                            return true; //dst_handle exists
                        }
                    } else {

                        FileInputStream fileInput = new FileInputStream(src_handle.file);
                        FileOutputStream fileOutput = new FileOutputStream(dst_handle.file);
                        BufferedInputStream bufferInput = new BufferedInputStream(fileInput);
                        BufferedOutputStream bufferOutput = new BufferedOutputStream(fileOutput);

                        //4k buffer
                        int blockSize = 4 * 1024 * 1024;
                        byte[] buffer = new byte[blockSize];

                        long numberOfBlocks = sizeInBytes / blockSize + 1;  //this number contains tail block, used to align blocks
                        long numberOfBlocksCopied = 0;

                        while (numberOfBlocksCopied < numberOfBlocks - 1) {

                            waitUntilNotInterrupt();//here is a waiting point，abortSignal is invalid here

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
                                monitor.receiveMessage(5, e.getMessage());
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
                } catch (FileNotFoundException ex) {
                    Log.e("core.Service", "File not found or some wrong happen");
                    monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED);
                    monitor.receiveMessage(0, "File not found");
                    return false;
                } catch (IOException e) {
                    //should not happen
                    monitor.receiveMessage(4, e.getMessage());
                    monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED);
                    return false;
                }
            }
            else{
                //abort signal arrives, but doesn't report message
                hasNotAborted=false;
                return false;
            }
        }


        /*
        @Description: 使用系统的服务进行复制，暂时不予以考虑
         */
        private boolean copyBySystemDependedService(){
            return false;
        }


        /*
        复制之前先通告monitor 任务id和任务详情，再进行复制，如果复制失败则此子任务id最终的状态会以Fail 结束
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
            if(!task_monitor.abortSignal()&&hasNotAborted) {

                FileHandle[] list = src_folder.listFiles();

                taskId++;

                //need to copy the folder first
                FileHandle dst_handle = new FileHandle(dst, "/" + src_folder.getName());
                FileHandle iterator_handle = dst_handle.clone();
                boolean src_folder_copy_result = copyByStream(src_folder, dst_handle, monitor, taskId); //create src_folder here, src_folder can either be a file

                //@notice: without src_folder the following files contained in src_folder cannot be created
                //error report has been completed in copyByStream(), so there is no need to report again;
                if (list != null && src_folder_copy_result) {

                    //breath first
                    for (int i = 0; i < list.length; i++) {
                        taskId++;
                        iterator_handle.pointTo(dst_handle, "/" + list[i].getName());
                        copyByStream(list[i], iterator_handle, monitor, taskId);
                    }

                    //filter the list and get folders

                    FileHandle[] folders;
                    Object[] rawArray = SimpleArrayFilter.filter(list, new FileHandleFilter() {
                        @Override
                        public boolean accept(FileHandle handle) {
                            return handle.isDirectory();
                        }
                    });
                    folders = new FileHandle[rawArray.length];
                    for (int i = 0; i < rawArray.length; i++) {
                        folders[i] = (FileHandle) rawArray[i];
                    }


                    //copy continues recursively
                    for (int i = 0; i < folders.length; i++) {
                        //dst_handle.pointTo();
                        taskId = copyByStreamRecursively(taskId, folders[i], dst_handle, monitor);
                    }
                }
            }
            else{
                needToRollBack=true;
                hasNotAborted=false;
            }

            return taskId;

        }

        private void waitUntilNotInterrupt(){
            while(task_monitor.interruptSignal()){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    task_monitor.receiveMessage(9,e.getMessage());
                    task_monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                    return;
                }
            }
        }

        //当前撤销操作暂时不提供成功验证。
        private void rollBack() {

        }
    }

    private class MoveTask implements Runnable {
        private ProgressMonitor<String,Long> monitor;
        private FileHandle src;
        private String dstPath;
        private boolean allowRollBack=true;
        private boolean needToRollBack=false;

        public MoveTask(FileHandle arg_src,String dstPath,ProgressMonitor<String,Long> monitor){
            this.src=arg_src;
            this.dstPath=dstPath;
            this.monitor=monitor;
        }


        @Override
        public void run() {
            if(isInSameVolume(src,dstPath)){
                //rewrite absolute path directly
                if(!monitor.abortSignal()){
                    src.file.renameTo(new File(dstPath));
                    monitor.onFinished();
                }
            }
            else{
                //copy first then delete

//                FileHandle dst=new FileHandle(dstPath);
//                CopyTask cpTask=new CopyTask(0,internal_copy_monitor,src,dst,true);
//                cpTask.run();
//
//
//                //check internal_copy_monitor's status to ensure result
//                if(internal_copy_monitor.getProgressStatus()!= ProgressMonitor.PROGRESS_STATUS.COMPLETED){
//                    //something wrong happens, report to upper
//
//                }
//

            }

        }

        private void rollBack(){

        }

        //判断文件所在路径是否在dstPath所指定的卷中
        private boolean isInSameVolume(FileHandle f1,String dstPath){
            boolean[] f1_Volumeb={
                f1.isInStorageVolume(),f1.isInSDCardVolume()
            };
            boolean[] dst_Volumeb= {
                    dstPath.startsWith(f1.getVolumePrefix()) && f1_Volumeb[0],
                    dstPath.startsWith(f1.getVolumePrefix()) && f1_Volumeb[1]
            };

            return (f1_Volumeb[0]&&dst_Volumeb[0]) || (f1_Volumeb[1]&&dst_Volumeb[1]);
        }


    }
}

