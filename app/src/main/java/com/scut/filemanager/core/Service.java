package com.scut.filemanager.core;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.scut.filemanager.core.internal.CopyTaskMonitor;
import com.scut.filemanager.core.internal.MessageEntry;
import com.scut.filemanager.util.SimpleArrayFilter;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Description: 这是一个后台服务类对象，用于提供基本的文件访问操作服务
 * app启动的时候，通过Service.getInstance(Activity context)绑定服务，
 * 在获取服务实例后，通过服务的getStatus() 方法，判断服务启动情况，对应的
 * 服务启动结果由Service.SERVICE_STATUS枚举得到。
 *
 * <p>我们主要通过服务类来:<br> 获取活动的上下文，获取app的外部存储的私有目录，
 * 以及在外部存储路径下的某个子路径的文件句柄结果，一般我们通过getStorageDirPathName()获取到
 * 一个根目录的文件句柄后，即可从该文件句柄出发，索引其他文件句柄对象
 * </p>
 *
 * <p>
 *     为了与用户称呼统一，以下将内部存储卡称为内部存储（Internal Storage）。sdcard称谓不变
 * </p>
 * <p>
 *     安卓存储卷分为内部存储和外部存储，用户常说的内部存储（内存)虽然看上去是固化在手机内部
 *     但实际上也还是外部存储，只是安卓利用了linux文件系统的挂载的概念，将其挂载到了一个叫
 *     /storage的目录下了。既然是挂载那么获取到的目录就可以是一个列表，因为可以挂载多个存储卷
 *     但实际上，就目前而言，android设备在出厂的时候已经默认挂载了一个存储卷，加上用户后来加的
 *     sdcard，总共就两个。不过就目前的趋势来看，搭载安卓的设备已经开始逐步加大了对sd卡的限制
 *     有些机型甚至不能使用sdcard扩展容量。
 * </p>
 */

public class Service {

    /**
     * Description:服务的复制选项枚举.<br>
     *     可选操作有，替换已存在；不修改元属性；不跟随符号链接进行复制，
     *     递归复制文件夹
     */

    public enum Service_CopyOption{
        REPLACE_EXISTING,   //替换已存在
        COPY_ATTRIBUTE,    //不修改mtime
        NOT_FOLLOWINGLINK, //不跟随符号链接
        RECURSIVE_COPY //递归复制文件夹
    }

    private Service(android.app.Activity app_context){
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

    /**
     * Description:区别服务启动状态的枚举对象。OK 为一切正常
     * <p><br> 包括有： OK（正常状态）,READ_ONLY(只读状态）,
     * SDCARD_UNMOUNTED(sd卡未挂载），SDCARD_MOUNTED（sd卡已挂载）SDCARD_UNKNOWN（sd未知错误）
     * SDCARD_EXCEPTION(sd卡异常，尝试重新检测）
     * UNABLE_TO_READ_FILE_LIST, （即无法打开目录查看信息，）这与可读情况不同，
     * EXCEPTION:意外情况，主要用于调试，不在实际应用代码中使用
     * </p>
     */

    public enum SERVICE_STATUS{
        OK,
        READ_ONLY,              //检测到只读
        SDCARD_UNMOUNTED, SDCARD_MOUNTED,SDCARD_UNKNOWN,SDCARD_EXCEPTION,
        UNABLE_TO_READ_FILE_LIST,   //检测到不可读取文件列表
        EXCEPTION                   //意外情况，用于调试
    }

    /**
     * Description: 获取一个绑定至对应活动的服务类
     * @param app_context 提供程序上下文
     */
        
    public static Service getInstance(android.app.Activity app_context){
        if (!(svc instanceof Service)) {
            svc = new Service(app_context);
        }
        return svc;
    }

    /**
     * Description: Service对象关于程序上下文的反射，在后续的控制器中会经常使用。
     * getContext()获取服务类绑定的对应的android.app.Activity类，可省去函数传入多个参数的
     * 麻烦。
     */

    public android.app.Activity getContext(){
        return context;
    }


    /**
     * Description:app 启动前，应该去检查基本的服务启动状态。
     * 这个状态是对应内部存储的可操作状态的描述
     * 查看{@link SERVICE_STATUS}了解可返回的结果
     */
    public SERVICE_STATUS getStatus(){
        return status;
    }


    /**
     * Description: 获取关于SDCard状态的描述，由于android的安全机制，即使获得了写权限，也会有所限制。
     */
    public SERVICE_STATUS getSdcardStatus(){
        return sdcard_status;
    }




    /**
     * Description: 返回根目录绝对路径名. 注意，这里的根目录指代/storage/emulated/0
     */

    public String getStorageDirPathName() {
        return storage_emulated_0.getAbsolutePath();
    }

    /**
     * Description:返回内部存储的根目录的文件句柄
     */

    public FileHandle getStorageDirFileHandle(){
        return new FileHandle(storage_emulated_0);
    }

    /*
    * 这里的internal 和external 都是相对于内部存储卡和sd卡而言的
    * */
    /**
     * Description: 返回内部存储中的私有目录的绝对路径名
     *
     * <p>
     *     这里的internal 和external 都是相对于内部存储卡和sd卡而言的.
     *     在根据下的/data /system  我们暂时不需要管理
     * </p>
     * @return internal_storage_prefix/android/app/$package_name$ 这个目录会
     * 以包名结尾
     */

    public String getInternalPrivateDirectoryPathName(){
        return app_private_internal_dir.getAbsolutePath();
    }

    /**
     * Description:返回在sd卡中的私有目录
     *
     */
        
    public String getExternalPrivateDirectoryPathName(){
        return app_private_external_dir.getAbsolutePath();
    }

    /**
     * Description:返回SD卡根目录名称
     */

    public String getSDCardRootDirectoryPathName(){
        if(sdcard_status==SERVICE_STATUS.SDCARD_MOUNTED){
            return storage_sdcard.getAbsolutePath();
        }
        else{
            return null;
        }
    }

    /**
     * Description:返回SD卡根目录句柄, 如果状态不合法则返回null
     *
     */

    public FileHandle getSDCardRootDirectoryFileHandle() {
        if(sdcard_status==SERVICE_STATUS.SDCARD_MOUNTED){
            return new FileHandle(storage_sdcard);
        }
        else{
            return null;
        }
    }


    /**
     * Description:方便用于快速获取一个在内存存储卡下的路径，注意输入路径pathname
     *     前不需要以 “/” 开头
     * @param pathname 不需要"/"前缀
     * @return /storage/emulated/0/pathname
     */

    public  String getPathUnderRootDir(String pathname){
        String temp=storage_emulated_0.getAbsolutePath()+"/";
        return temp.concat(pathname);
    }


    /**
     * Description:获取在内部存储前缀下的关于pathname的文件句柄对象
     * @param pathname 不完整的从内部存储根目录出发的路径名
     */

    @RequiresApi(api = Build.VERSION_CODES.O)
    public  FileHandle getFileHandleUnderRootDir(String pathname){
        String path=getPathUnderRootDir(pathname);
        return new FileHandle(path);
    }

    //返回结果为 /storage/SD_CARD/pathname
    /**
     * Description:获取在sd卡根路径前缀下的关于pathname的完整路径名
     * @param pathname 不完整的从sd卡根目录出发的路径名
     */

    public String getPathUnderSdCard(String pathname){
        String temp=storage_sdcard.getAbsolutePath()+"/";
        return temp.concat(pathname);
    }

    /*
    @Description: 复制文件或文件夹到指定位置，需要提供目标路径和源文件，以及监视器和复制选项
    @Notices 请确保目标的有效性，dstPath必须是存在的路径，且是一个目录,该目录不能够是源文件的目录及其子目录。
    onFinished()在这里仅用于确认线程退出,
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
        在复制过程中，receiveMessage和onSubTaskStop()都是可选路径，即不一定会被调用。
        如果目标文件夹所在分区容量不足，同样会调用receiveMessage和onStop()通报监视器
        如果时复制过程中的产生的错误，onStop不会调用，而是调用onSubTaskStop()，因为这样，可以知道是哪个文件
        的操作对应的任务产生了异常
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
        onFinished()

    abortSignal() 与abortSignal(int slot)
    默认丢弃使用abortSignal()检测，同时使用1号终止信号槽决定回滚是否应该终止
    如果设置了回滚，最后会通过receiveMessage(code,msg),通告监视器回滚情况

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
        9=InterruptedException（这是java类的错误）
        10=回滚情况

     */
    /**
     * Description:复制文件或文件夹到指定位置，需要提供目标路径和源文件，以及监视器和复制选项
     * <p>
     *     <em>Notice:</em> 请确保目标的有效性，dstPath必须是存在的路径，且是一个目录,该目录不能够是源文件的目录及其子目录。
     * 复制一个目录到其子目录会发生很严重的不可收敛问题。
     *     onFinished()在这里仅用于确认线程退出
     *     </p>
     * @param         src: 源文件句柄，可以是单个文件或者一个文件夹，当然传入的目标如果是不存在则该任务不会被执行。
     * @param         dstPath: 目标路径，如果目标路径同样需要经过检验，需要是可写且存在的路径
     * @param          monitor: 监视器对象，键值对类型为： <String,Long> 分别对应文件名和大小(字节)
     * @param         allowRollBack: 用于设置当丢弃复制操作时是否进行抹去已复制内容的操作
     * @param         options : Service_CopyOption枚举类型，目前只有RECURSIVE_COPY 和REPLACE_EXISTING可被解析
     * <p>
     *                        <em>Protocols: </em>
     *                        <p>
     *         任务开始后，先统计需要复制的文件的总大小totalSize，并以onProgress(K,V)函数，返回"totalSize":srcSize
     *         到进度监视器中。
     *         子任务开始时调用onSubTaskStart(), 并通过describeTask 设置子任务的title
     *         在复制过程中，receiveMessage和onSubTaskStop()都是可选路径，即不一定会被调用。
     *         如果目标文件夹所在分区容量不足，同样会调用receiveMessage和onStop()通报监视器
     *         如果时复制过程中的产生的错误，onStop不会调用，而是调用onSubTaskStop()，因为这样，可以知道是哪个文件
     *         的操作对应的任务产生了异常
     *         生命周期函数调用概要:
     *         onStart()->空间余量检测()
     *         [{
     *         onSubTaskStart(taskid)->describeTask(taskid,title)->
     *         [onSubProgress(FileAbsolutePathName,numberOfBytesCopied)*->]
     *         [
     *         receiveMessage(MsgCode,message)->
     *         onSubTaskStop(PROGRESS_STATUS.FAILED)
     *         ->]
     *         onSubTaskFinish(taskid)->
     *         }*]
     *         ->[receiveMessage(MsgCode,message)->onStop(PROGRESS_STATUS.FAILED)->]
     *         onFinished()
     *                        </p>
     *                        <p>
     *                            abortSignal() 与abortSignal(int slot)
     *     默认丢弃使用abortSignal()检测，同时使用1号终止信号槽决定回滚是否应该终止
     *     如果设置了回滚，最后会通过receiveMessage(code,msg),通告监视器回滚情况
     *                        </p>
     *                        <p>
     *                            信息码描述：
     *         receiveMessage(int code,String msg):
     *         0=找不到文件
     *         1=目标路径不可写
     *         2=目标路径不存在
     *         3=源文件不可读
     *         4=流关闭错误
     *         5=读写错误
     *         6=创建文件夹失败
     *         7=空间不足
     *         8=未知错误
     *         9=InterruptedException（这是java类的错误）
     *         10=回滚情况
     *                        </p>
     * </p>
     */

    public void copy(FileHandle src, String dstPath, ProgressMonitor<String,Long> monitor, boolean allowRollBack,Service_CopyOption... options){
        boolean replace_existing,copy_attribute,not_following_link,recursive_copy;
        replace_existing=parseOption(Service_CopyOption.REPLACE_EXISTING,options);
        copy_attribute=parseOption(Service_CopyOption.COPY_ATTRIBUTE,options);
        not_following_link=parseOption(Service_CopyOption.NOT_FOLLOWINGLINK,options);
        recursive_copy=parseOption(Service_CopyOption.RECURSIVE_COPY,options);

        //testCode-------------------------------------------------------
        FileHandle dst=new FileHandle(dstPath);
        monitor.onStart(); //onStart() 标志任务已被接受，比起在工作线程中执行onStart,可以有效防止工作线程未开始，任务因特殊原因就结束了
        if(!dst.isDirectory()){
            return; //exit point
        }
        else if(!this.validateCopyDstPath(src,dstPath)){ //验证不通过
            monitor.receiveMessage(MessageCode.DEST_PATH_CANNOT_CONTAIN_SRC_FOLDER,"destPath cannot contain source folder and it subfolder");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            monitor.onFinished();
            return;
        }


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


    /**
     * Description:传入多个FileHandle对象进行复制
     * @param src FileHandle对象数组，不可空
     */

    public void copy(FileHandle[] src,String dstPath,ProgressMonitor<String,Long> monitor,boolean allowRollBack,Service_CopyOption... options){
        boolean argument_test=true;
        for (FileHandle srcFile :
                src) {
            argument_test  &= this.validateCopyDstPath(srcFile,dstPath);
        }
        //暂时不对src的存在性做检测

        monitor.onStart();
        if(argument_test) {
            FileHandle dst = new FileHandle(dstPath);
            CopyTask copyTask=new CopyTask(src,dst,0,monitor,false);
            svc_executor.execute(copyTask);
        }
        else{
            monitor.receiveMessage(MessageCode.DEST_PATH_CANNOT_CONTAIN_SRC_FOLDER,"destPath cannot contain source folder and it subfolder");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            monitor.onFinished();
        }
    }


    /**
     * Description:移动文件或文件夹到指定位置，需要提供目标路径和源文件，以及监视器
     * @param src 源文件句柄，可以是单个文件或者一个文件夹，当然传入的目标如果是不存在则该任务不会被执行。
     * @param dstPath 目标路径，如果目标路径同样需要经过检验，需要是可写且存在的路径
     * @param option  仅需按照需要设置为REPLACE_EXISTING 即可，应该由业务逻辑判断目标是否存在，因为ProgressMonitor对交互不太好.
     * @param monitor 监视器对象，键值对类型为： <String,Float> 分别对应文件名和进度值
     * <p>
     *                     <em>Protocols:</em><br>
     *                             调用路径
     *         任务启动时，会调用onStart()标记启动状态
     *         任务开始前，如果移动操作为存储卷之间的移动，则会先进行空间余量检测，不通过则任务会自动终止
     *         任务进行时，会调用onProgress(当前正在操作的文件绝对路径名，进度值）
     *         进度值取值为：0.0~1.0
     *         任务异常时，会通过监视器的receiveMessage(code,msg)陆续通告消息，并伴随onStop(PROGRESS_STATUS.FAILED)
     *         的调用。
     *         任务正常结束则调用onFinished(), 否则将以onStop(某状态)来结束
     *
     *         使用到的方法：
     *         receiveMessage(code,msg)
     *         onStart() [onStop()] [onFinished()]
     *         onProgress()
     * </p>
     * <p>
     *                     <h4>信息码描述：</h4>
     *        详细参照Service.MessageCode类
     *         receiveMessage(int code,String msg):
     *         0=找不到文件
     *         1=目标路径不可写
     *         2=目标路径不存在
     *         3=源文件不可读
     *         11=目标已存在该文件，且未设置覆写状态
     *
     *         //仅在不同挂载设备间复制时会出现
     *         4=流关闭错误
     *         5=读写错误
     *         6=创建文件夹失败
     *         //----------------------------
     *
     *         7=空间不足
     *         8=未知错误
     *         9=InterruptedException
     * </p>
     *
     */
    public void move(FileHandle src, String dstPath,Service_CopyOption option,ProgressMonitor<String,Float> monitor){
        //two type of dstPath
        FileHandle dstFolder=new FileHandle(dstPath);

        if(!dstFolder.isDirectory()||!dstFolder.isExist()){
            monitor.receiveMessage(1,"destination isn't a folder");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            return;
        }
        else if(!src.isExist()){
            monitor.receiveMessage(0,"source file cannot be found");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            return;
        }

        MoveTask moveTask=new MoveTask(src,dstPath,option,monitor);
        svc_executor.execute(moveTask);
    }

    /**
     * Description:传入多个FileHandle进行复制
     *
     */
    public void move(List<FileHandle> selections,String dstPath,Service_CopyOption option,ProgressMonitor<String,Float> monitor){
        monitor.onStart();
        FileHandle dstFolder=new FileHandle(dstPath);
        if(!dstFolder.isDirectory()||!dstFolder.isExist()){
            monitor.receiveMessage(1,"destination isn't a folder");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            return;
        }
        else if(selections.size()>0&& !selections.get(0).isExist()){
            monitor.receiveMessage(0,"source file cannot be found");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            return;
        }

        MoveTask moveTask=new MoveTask(selections,dstPath,option,monitor);
        svc_executor.execute(moveTask);
    }


    /**
     * Description: 判断给定路径是否存在文件，如果路径不合法，则返回false
     * 合法性检测也可通过{@link FileHandle}中的方法进行判断
     */
    public boolean ExistsAtPath(String pathname){
        if(isLegalPath(pathname)){
            FileHandle target=new FileHandle(pathname);
            return target.isExist();
        }
        else {
            return false;
        }
    }



    /**
     * Description:判断给定路径是否合法,其中合法情况为：<br>
     *     1.目标存在于：
     *     /storage/emulated/0
     *     /storage/$sd_card$/
     *     2.同时目标可访问(accessible)
     *     判断路径是否合法后应该加入非法字符的判断，这里没有对非法字符进行检测
     *     是否含有非法字符的检测见FileHandle类的静态方法containIllegalChar(String args)
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

    /**
     * Description:获取sdcard的型号,当然如果sdcard未挂载，返回null
     */
    public String getSDCardModel(){
        StringBuilder sdcard_model_strBuilder=new StringBuilder(getSDCardRootDirectoryPathName());
        if(sdcard_model_strBuilder.length()!=0){
            int cut_pos=sdcard_model_strBuilder.lastIndexOf("/");
            return sdcard_model_strBuilder.substring(cut_pos+1,sdcard_model_strBuilder.length());
        }
        return null;
    }

    /**
     * Description:以服务级别来判断一个给定路径是否在sdcard 下，如果sd卡未挂载，则会返回false
     * 之所以以服务级别来判断，是因为FileHandle无法在Service服务类启动前知道程序的上下文。
     */
        
    public boolean isUnderSDCardPath(@NonNull String pathname){
        String sdcard_str=getSDCardRootDirectoryPathName();
        return pathname.startsWith(sdcard_str);
    }
    
    /**
     * Description:以服务级别来判断一个给定路径是否在内存存储路径下，如果服务状态不为OK,则返回状态不确定
     */
        
    public boolean isUnderStoragePath(@NonNull String pathname){
        String storage_pathname= getStorageDirPathName();

        return pathname.startsWith(storage_pathname);
    }

    /*
    @Description: 获取内置存储卡的存储空间的大小，单位为字节
    如果需要转换单位或增添转换所需的工具类，参见filemanager.utiL包中的方法
     */
    /**
     * Description:获取内置存储卡的存储空间的大小，单位为字节
     *     如果需要转换单位或增添转换所需的工具类，参见filemanager.util包中的方法
     */
        
    public long getStorageTotalCapacity(){
        return storage_emulated_0.getTotalSpace();
    }

    /**
     * Description:获取内存存储的剩余可用空间
     * 如果需要转换单位或增添转换所需的工具类，参见filemanager.util包中的方法
     */
        
    public long getStorageFreeCapacity(){
        return storage_emulated_0.getFreeSpace();
    }
    
    /**
     * Description:获取内存存储中剩余可用空间，这个值对应File.getUsableSpace的结果，这个值更加精确
     */
        
    public long getStorageUsableCapacity(){
        return storage_emulated_0.getUsableSpace();
    }

    /**
     * Description:获取Sd卡的总容量大小，返回单位为字节
     */
        
    public long getSdcardTotalCapacity(){
        return storage_sdcard.getTotalSpace();
    }

    /**
     * Description:获取Sd卡的可用空间大小，返回单位为字节
     */

    public long getSdcardFreeCapacity(){
        return storage_sdcard.getFreeSpace();
    }

    /*
        @Description:        0=找不到文件
        1=目标路径不可写
        2=目标路径不存在
        3=源文件不可读
        4=流关闭错误
        5=读写错误
        6=创建文件夹失败
        7=空间不足
        8=未知错误
        9=InterruptedException（这是java类的错误）
        10=回滚情况
    */

    /**
     * Description:提供给进度监视器的消息码，通过消息码可判断对应消息的类型
     * 消息都是封装为{@link MessageEntry}键值项
     */

    static public final class MessageCode{

        /**
         * Description:找不到源文件
         */
        static public final int FILE_NOT_FOUND=0;
        /**
         * Description: 目标路径不可写
         */
        static public final int DEST_CANNOT_WRITE=1;
        /**
         * Description:找不到目标路径。
         * 一般是因为destinationPath参数错误导致，也可能是因为destinationPath对应的文件没有互斥，导致在检测其存在性前
         * destPath被删除了
         */
        static public final int DEST_NOT_FOUND=2;
        /**
         * Description:源文件不可读，即创建读取流时失败了
         */
        static public final int SOURCE_CANNOT_READ=3;
        /**
         * Description:流因为占用不可关闭，容易引起内存泄露
         */
        static public final int STREAM_CLOSE_ERROR=4;
        /**
         * Description:读写错误，这是在通过流读取写入文件时发生的错误
         */
        static public final int READ_WRITE_ERROR=5;
        /**
         * Description:创建文件夹失败，如果创建文件夹失败，将会导致其子目录下的文件也相继失效，
         * 因此复制服务在继续复制前就终止了更多的错误发生。
         */
        static public final int MKDIR_FAILS=6;
        /**
         * Description: 没有足够空间，在服务进行任何复制操作前都会都会检测可用空间情况
         */
        static public final int NO_ENOUGH_SPACE=7;
        /**
         * Description: 未知错误，标记超出程序理解范围的错误
         */
        static public final int UNKNOWN_ERROR=8;
        /**
         * Description: 进程休眠的中断错误，这是一个java类错误，一般出现在暂停阶段
         */
        static public final int INTERRUPTED_EXCEPTION_WHILE_WAITING=9;
        /**
         * Description: 回归情况汇报，这不是一个错误信息码
         */
        static public final int ROLL_BACK_REPORT=10;
        /**
         * Description: 目标路径已存在文件，如果复制选项没有设置替换存在，则会抛出此错误
         */
        static public final int DEST_EXIST_FILE=11;
        /**
         * Description: 递归复制错误，在服务启动时，会检查目标路径是否为源目录的子目录，如果是，则拒绝
         * 该复制请求，并向其发送该错误信息。
         */
        static public final int DEST_PATH_CANNOT_CONTAIN_SRC_FOLDER=12;
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
    private static android.app.Activity context;

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

    private boolean validateCopyDstPath(FileHandle src,String dstPath){
        if(src.isDirectory()){
            return !dstPath.startsWith(src.getAbsolutePathName());
        }
        return true; //src 不是文件夹，安全操作
    }
    private class CopyTask implements Runnable{

        protected  ProgressMonitor<String,Long> task_monitor;
        protected FileHandle src,dst;
        protected FileHandle[] sources;
        protected boolean needToRollBack=false;
        protected boolean allowRollBackFlag=true;
        protected boolean hasNotAborted=true; //prevent inconsistent
        private boolean copy_result=true;
        private boolean peace_out=false;
        private boolean isMultipleSrc=false;
        int pid;

        //arg_dst 为目标目录
        public CopyTask(int initial_taskId,ProgressMonitor<String,Long> arg_monitor,FileHandle arg_src,FileHandle dstFolder,boolean allowRollBackFlag){
            pid=initial_taskId;
            task_monitor=arg_monitor;
            dst=dstFolder;
            src=arg_src;
            this.allowRollBackFlag=allowRollBackFlag;
        }

        public CopyTask(FileHandle[] src, FileHandle dstFolder, int initial_taskId,ProgressMonitor<String,Long> monitor,boolean allowRollBackFlag){
            this.isMultipleSrc=true;
            sources=src;
            pid=initial_taskId;
            task_monitor=monitor;
            dst=dstFolder;
            this.allowRollBackFlag=allowRollBackFlag;

        }



        @Override
        public void run() {
            //check whether destination has enough space before performing copying
            long srcSize=0L;
            try{
                if(!isMultipleSrc) {
                    srcSize = src._totalSize();
                }
                else {
                    for (int i = 0; i < sources.length; i++) {
                        srcSize+=sources[i]._totalSize();
                    }
                }
            }
            catch (IOException ioex){
                task_monitor.receiveMessage(3,ioex.getMessage());
                task_monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);

                return;
            }

            //检查剩余空间
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
            if(isMultipleSrc){
                for (int i = 0; i < sources.length; i++) {
                    pid=copyByStreamRecursively(pid,sources[i],dst,task_monitor);
                }
            }
            else {
                copyByStreamRecursively(pid, src, dst, task_monitor);
            }

            if(needToRollBack && allowRollBackFlag ){
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
            if(hasNotAborted && !monitor.abortSignal()) {

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
                                monitor.receiveMessage(MessageCode.MKDIR_FAILS, "creating directory fails");
                                monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED); //createDirectory fails
                            }
                            //monitor.onSubTaskFinish(taskId);
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

                        //4M buffer
                        int blockSize = 4 * 1024 * 1024;
                        byte[] buffer = new byte[blockSize];

                        long numberOfBlocks = sizeInBytes / blockSize + 1;  //this number contains tail block, used to align blocks
                        long numberOfBlocksCopied = 0;

                        while (numberOfBlocksCopied < numberOfBlocks - 1) {

                            waitUntilNotInterrupt();//here is a waiting point，abortSignal is invalid here
                            if(!hasNotAborted){ //循环内部设置退出检查点。
                                return false; //退出点，防止以下代码继续执行
                            }

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
                                monitor.receiveMessage(MessageCode.READ_WRITE_ERROR, e.getMessage());
                                monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED);

                                bufferOutput.close();
                                bufferInput.close();
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
                                numberOfBytesCopied += tailLength;

                                monitor.onSubProgress(taskId, "numberOfBytesCopied", numberOfBytesCopied);
                            } catch (IOException e) {
                                Log.e("core.Service", "copying file by stream has incurred an IOException" + " Exception message: " +
                                        e.getMessage());
                                monitor.receiveMessage(5,e.getMessage());
                                monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED);

                                bufferInput.close();
                                bufferOutput.close();

                                return false;
                            }
                        }


                        bufferInput.close();
                        bufferOutput.close();
                        monitor.onSubTaskFinish(taskId);
                        return true;
                    }
                } catch (FileNotFoundException ex) {
                    Log.e("core.Service", "File not found or some wrong happen");
                    monitor.onSubTaskStop(taskId, ProgressMonitor.PROGRESS_STATUS.FAILED);
                    monitor.receiveMessage(MessageCode.FILE_NOT_FOUND, "File not found");
                    return false;
                } catch (IOException e) {
                    //should not happen
                    monitor.receiveMessage(MessageCode.STREAM_CLOSE_ERROR, e.getMessage());
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


//        private boolean validateSources(){
//            FileHandle previous=sources[0];
//            boolean isUnderSameFolder=true;
//            for (int i = 1; i < sources.length; i++) {
//                isUnderSameFolder=
//            }
//        }

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
            if(hasNotAborted && !task_monitor.abortSignal() ) {

                FileHandle[] list = src_folder.listFiles();

                taskId++;

                //need to copy the folder first
                FileHandle dst_handle = new FileHandle(dst, "/" + src_folder.getName());
                FileHandle iterator_handle = dst_handle.clone();
                boolean src_folder_copy_result = copyByStream(src_folder, dst_handle, monitor, taskId); //create src_folder here, src_folder can either be a file

                //@notice: without src_folder the following files contained in src_folder cannot be created
                //error report has been completed in copyByStream(), so there is no need to report again;
                if ((list != null&&list.length>0 )&& src_folder_copy_result) {

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
                    if(rawArray!=null&&rawArray.length>0){//safety check
                        folders = new FileHandle[rawArray.length];
                        for (int i = 0; i < rawArray.length; i++) {
                            folders[i] = (FileHandle) rawArray[i];
                        }


                        //copy continues recursively
                        for (FileHandle folder : folders) {
                            //dst_handle.pointTo();
                            taskId = copyByStreamRecursively(taskId, folder, dst_handle, monitor);
                        }
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
                    hasNotAborted=false;
                    task_monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                    task_monitor.onFinished();
                    return;
                }
            }
        }

        //当前撤销操作暂时不提供成功验证。
        private void rollBack() {
            boolean[] slot={false};
            task_monitor.setUpAbortSignalSlot(slot);
            if(dst.isExist()){
                boolean result=dst._deleteRecursively(slot);
                task_monitor.receiveMessage(10,String.valueOf(result));
            }
        }
    }

    private class MoveTask implements Runnable {
        private ProgressMonitor<String,Float> monitor;
        private FileHandle src;
        private List<FileHandle> listOfFiles=null;
        private boolean isMultiple=false;
        private String dstPath;
        private boolean allowRollBack=true;
        private boolean needToRollBack=false;
        private boolean replace_existing_flag=false;

        public MoveTask(FileHandle arg_src,String dstPath,Service_CopyOption option, ProgressMonitor<String,Float> monitor){
            this.src=arg_src;
            this.dstPath=dstPath;
            this.monitor=monitor;
            if(option==Service_CopyOption.REPLACE_EXISTING){
                replace_existing_flag=true;
            }
        }

        public MoveTask(List<FileHandle> list,String dstPath,Service_CopyOption option, ProgressMonitor<String,Float> monitor){
            this.listOfFiles=list;
            this.dstPath=dstPath;
            this.monitor=monitor;
            if(option==Service_CopyOption.REPLACE_EXISTING){
                replace_existing_flag=true;
            }
            this.isMultiple=true;
        }


        @Override
        public void run() {
            if(isMultiple){
                //rename directly here
                if(!monitor.abortSignal()){
                    boolean move_result=true;
                    int i=0;
                    for (FileHandle src1 :
                            listOfFiles) {
                        i++;
                        String dstSrcPath=dstPath+"/"+src1.getName();

                        //check existing
                        FileHandle dstFileHandle= new FileHandle(dstSrcPath);
                        if(dstFileHandle.isExist()) {
                            if (replace_existing_flag) {
                                boolean delete_result = dstFileHandle._deleteRecursively(null);
                                if (!delete_result) {
                                    monitor.receiveMessage(MessageCode.UNKNOWN_ERROR, "destination exists files and cannot be overwritten");
                                    monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                                    return;
                                }
                            } else { //没有设置replacing
                                monitor.receiveMessage(MessageCode.DEST_EXIST_FILE, "replace_existing option has no been set");
                                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                                return;
                            }
                        }
                        move_result=move_result& src1.file.renameTo(dstFileHandle.getFile());
                        monitor.onProgress(src1.getAbsolutePathName(),
                                (float)i/listOfFiles.size());
                    }
                    if(move_result) {
                        monitor.onFinished();
                    }
                    else{
                        monitor.receiveMessage(MessageCode.UNKNOWN_ERROR,"may be directory is not empty");
                        monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                    }
                }
                else{
                        monitor.onStop(ProgressMonitor.PROGRESS_STATUS.ABORTED);
                }
            }
            else{
                if(isInSameVolume(src,dstPath)){
                    //rewrite absolute path directly
                    if(!monitor.abortSignal()){
                        dstPath=dstPath+"/"+src.getName();

                        //check existing
                        FileHandle dstFileHandle= new FileHandle(dstPath);
                        if(dstFileHandle.isExist()) {
                            if(replace_existing_flag){
                                boolean delete_result=dstFileHandle._deleteRecursively(null);
                                if(!delete_result) {
                                    monitor.receiveMessage(MessageCode.UNKNOWN_ERROR,"destination exists files and cannot be overwritten");
                                    monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                                    return;
                                }
                            }
                            else{
                                monitor.receiveMessage(MessageCode.DEST_EXIST_FILE,"replace_existing option has no been set");
                                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                                return;
                            }
                        }
                        boolean move_result=src.file.renameTo(dstFileHandle.getFile());
                        if(move_result) {
                            monitor.onFinished();
                        }
                        else{
                            monitor.receiveMessage(8,"directory is not empty");
                            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                        }
                    }
                    else{
                        monitor.onStop(ProgressMonitor.PROGRESS_STATUS.ABORTED);
                    }

                }
                else{
                    //copy first then delete

                    FileHandle dst=new FileHandle(dstPath);

                    class InternalCopyMonitor extends CopyTaskMonitor{

                        private MessageEntry currentTask=new MessageEntry(0,"");

                        @Override
                        public void onSubTaskStop(int taskId, PROGRESS_STATUS status) {
                            this.progress_status=status;
                        }

                        @Override
                        public void onProgress(String key, Long value) {
                            if(key.contentEquals("totalSize")){
                                numberOfBytesNeedToCopy=value;
                            }
                        }

                        @Override
                        public void onSubProgress(int taskId, String key, Long value) {
                            this.getTracker().put(taskId,value);
                            monitor.onProgress(currentTask.getValue(),computeProgress());
                        }

                        @Override
                        public void describeTask(int taskId, String title) {
                            currentTask.setValue(title);
                            currentTask.setKey(taskId);
                        }

                        @Override
                        public void onFinished() {
                            if(progress_status==null){
                                progress_status=PROGRESS_STATUS.COMPLETED;
                            }
                        }

                        @Override
                        public boolean interruptSignal() {
                            return monitor.interruptSignal();
                        }

                        @Override
                        public boolean abortSignal() {
                            return monitor.abortSignal();
                        }

                        private float computeProgress(){
                            return (float)reportValueByTracker()/(float)numberOfBytesNeedToCopy;
                        }
                    }

                    InternalCopyMonitor internal_copy_monitor=new InternalCopyMonitor();

                    CopyTask cpTask=new CopyTask(0,internal_copy_monitor,src,dst,false);
                    cpTask.run();


                    //check internal_copy_monitor's status to ensure result
                    if(internal_copy_monitor.getProgressStatus()!= ProgressMonitor.PROGRESS_STATUS.COMPLETED){
                        //something wrong happens, report to upper

                        //消息传递
                        while(internal_copy_monitor.hasMessage()){
                            MessageEntry msgEntry=internal_copy_monitor.popMessageEntry();
                            monitor.receiveMessage(msgEntry.getKey(),msgEntry.getValue());
                        }

                        monitor.onStop(internal_copy_monitor.getProgressStatus());
                    }

                    //delete original files
                    boolean delete_result=src._deleteRecursively(null);
                    if(!delete_result){
                        //delete original file fail, report to monitor
                        monitor.receiveMessage(8,"unknown error");
                        monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                    }

                }
            }


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

