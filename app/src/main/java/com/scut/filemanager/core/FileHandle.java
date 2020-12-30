package com.scut.filemanager.core;

import android.util.Log;

import com.scut.filemanager.core.concurrent.SharedThreadPool;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;


public class FileHandle {
    //文件句柄类，处理与文件各种交互问题，包括元数据的编辑，查看etc
    //基于file的自定义接口类。
    //基本文件属性判断，RWX
    //文件的ctime, mtime,atime
    //文件的大小
    //文件的其他数据段，元数据：
    //

    File file; //not nullable
    String CanonicalPathName=""; //包含文件名在内的正则路径名,一般使用这个但是容易引发IO异常
    String AbsolutePathName="";     // 包含文件名在内的绝对路径名


    //暂时用不上
    enum FileAccessPosition{
        Owner,Group,User,CurrentID
        //CurrentID描述当前用户的身份，就是指应用本身。
    };

    /*
    @Description: 构造函数，这是对java.io.File类的封装类，鉴于我们可操作的目录路径
    FileHandle类希望能提供一些便利的函数来判断所封装的File对象是否具有意义，同时也为
    后续的某些常用操作提供铺垫。
    正常情况下,第一次调用getCanonicalPath返回的将是null，因为正则路径名需要进行IO操作才能
    进行获取，因此，为了规避过多的IO操作，FileHandle构造函数并不会对CanonicalPath进行初始
    化，如果需要用到CanonicalPath 可以考虑使用tryRetrieveCanonicalPath()这个函数。
     */

    public FileHandle(File f) throws NullPointerException {
        if(f==null)
            throw new NullPointerException("[FileHandle:Null pointer initialization]");
        file=f.getAbsoluteFile();
        AbsolutePathName=file.getAbsolutePath();
    }

    /*
    @Description:
     */
    public FileHandle(String pathname){
        file=new File(pathname);
        //CanonicalPathName=file.getCanonicalPath();
        AbsolutePathName=file.getAbsolutePath();
    }

    /*
    @Description: 此构造函数用于拼接目录前缀与文件路径名
    使用此方法产生的CanonicalPath无更新，除非重新调用tryRetrieveCanonicalPath()函数
     */
    public FileHandle(FileHandle dir,String pathname){
        if(dir.isDirectory()){
            file=new File(dir.getAbsolutePathName()+pathname);
            AbsolutePathName=dir.getAbsolutePathName()+pathname;
            //CanonicalPathName=dir.getCanonicalPathName()+pathname; //notice this variable of how it comes
        }
        else{
            file=dir.file;
            AbsolutePathName=dir.getAbsolutePathName();
            //CanonicalPathName=dir.getCanonicalPathName();
        }
    }

    public File getFile(){
        return file;
    }

    /*
    @Description: 该方法重定向句柄所处理的抽象路径名，适当使用此方法可以达到复用迭代对象的目的
     */
    public void pointTo(FileHandle dir,String pathname){
            file=new File(dir.getAbsolutePathName()+pathname);
            AbsolutePathName=dir.getAbsolutePathName()+pathname;
            CanonicalPathName=dir.getCanonicalPathName()+pathname; //notice this variable of how it comes
    }

    public String getAbsolutePathName(){
        return AbsolutePathName;
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


    /*
    @Description: 统计文件夹的总大小时，可能会阻塞主线程，因此这里将它线程化。
    @Return: Future<Integer> 通过Future<Long> 可尝试取消任务并获取实时计算信息
    @Protocol:
        关于返回对象Future<Long> 的接口描述：
        > boolean cancel(boolean mayInterruptIfRunning)
            发出中断信号，并返回发出中断信号后，到函数结束时的取消结果，该方法未非阻塞方法，如果需要判断是否已经中断
            需要通过isCancelled() 进行重复检测
            调用该方法后，isDone()将返回true
        > isCancelled()
            判断取消任务是否成功，如果该方法返回true,则isDone()方法也会返回true
        > isDone()
            判断任务是否完成
        > Long get()
            获取当前已统计的文件数目，需要注意的是，如果cancel() 被调用，则该方法返回值将可能
            就此稳定下来
        > Long get(long l,TimeUnit timeUnit) throws InterruptException
            使当前线程等待一段时间后，再去获取统计值
            timeUnit 可设定多种值{
                    DAYS
                    时间单位代表二十四小时
                    HOURS
                    时间单位代表六十分钟
                    MICROSECONDS
                    时间单位代表千分之一毫秒
                    MILLISECONDS
                    时间单位为千分之一秒
                    MINUTES
                    时间单位代表60秒
                    NANOSECONDS
                    时间单位代表千分之一千分之一
                    SECONDS
                    时间单位代表一秒
            }
            如果在等待过程中，这个线程被中断，get方法会抛出InterruptException,使用时应注意
            此异常的合理处理

    */
    public Future<Long> totalSize(){
        FileSizeCounterTask fileSizeCounter=new FileSizeCounterTask(this );
        sharedThreadPool.executeTask(fileSizeCounter,SharedThreadPool.PRIORITY.LOW);
        return fileSizeCounter;
    };

    /*
    @Description: totalSize的阻塞方法,一般不建议在主线程中使用
     */
    public long _totalSize() throws IOException{
        if(isDirectory()){
            long sum=this.Size();
            FileHandle[] list_of_files=listFiles();
            if(list_of_files!=null) {
                for (int i = 0; i < list_of_files.length; i++) {
                    sum+=list_of_files[i]._totalSize();
                }
                return sum;
            }
            else{
                //should not happen
                throw new IOException("reading directory failed");
            }

        }
        else{
            return this.Size();
        }
    }

    /*
    @Description: 该方法用于尝试获取一次正则路径，获取正则路径需要进行一次IO操作，但这里的实现
    把可能的IO异常限制在此方法内，并返回异常错误信息
    @Return: 无错误时返回值为null,有错误则返回错误信息。
     */
    public String tryRetrieveCanonicalPath(){
        String exMsg=null;
        try{
            CanonicalPathName=file.getCanonicalPath();
        }
        catch(IOException ioex){
            CanonicalPathName=null;
            exMsg=ioex.getMessage();
            Log.e("FileHandle","tryRetrieveCanonicalPath failed");
        }
        return exMsg;
    }



    /*
    @Description: 获取文件当前所在路径正则路径名,需要注意的是，有时在获取正则路径名时会
    遭遇失败，比如因为在查询CanonicalPath是需要进行文件系统的查询工作
    @Nullable
     */
    public String getCanonicalPathName()  {
        return CanonicalPathName;
    }


    public String getParentPathNameByAbsolutePathName(){
        int last=AbsolutePathName.lastIndexOf('/');
        return AbsolutePathName.substring(0,last);
    }

        /*
            @Description: 从表示该文件句柄的抽象路径名出发，获取其父文件的抽象路径名的正则路径名
            由于获取的是正则路径名，有可能在转换相对路径的过程中会产生IO错误，不过这种错误主要是
            对于符号链接所表示的抽象路径名而言的。
            @Return: 父目录的正则路径名，如/home/dir/fix, 则返回/home/dir
            @Notice: 如果该文件不存在父目录，则返回值为空，建议不要直接使用该函数作为判断目录是否已经达到
            根节点的依据，因为大部分返回空的情况都是已经返回到安卓根节点的情况，但是我们的操作空间并不在于
            根目录"/" 下 ，而是/storage/emulated/0 和sd卡目录，详情查看isAndroidRoot()
     */
    public String getParentNameByParentFile(){
        File parent=file.getParentFile();
        if(parent==null){
            return null;
        }
        try {
            String parent_pathname=parent.getCanonicalPath();
            return parent_pathname;
        }
        catch(IOException ioex){
            Log.w("FileHandle","retrieved parent's canonical path file");
            return null;
        }
    }

    //如果不存在ParentFile则返回空
    public FileHandle getParentFileHandle(){
        File parent=file.getParentFile();
        if(parent==null){
            Log.w("FileHandle","this file has no parent");
            return null;
        }
        return new FileHandle(parent);
    }

    /*
        @Description: 获取目录下的文件数
     */
    public int getFileCount() {
        if(isDirectory()){
            File[] listFiles=file.listFiles();
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

    public boolean isAndroidRoot(){
        return isStorageRoot()||isSdCardRoot();
    }

    public boolean isSameParentAs(FileHandle fileHandle){
        return this.getParentPathNameByAbsolutePathName().
                contentEquals(fileHandle.getParentPathNameByAbsolutePathName());
    }

    public boolean isStorageRoot(){
        return AbsolutePathName.contentEquals(storage0_prefix);
    }

    public boolean isSdCardRoot(){
        if(sdcard_prefix!=null){
            return AbsolutePathName.contentEquals(sdcard_prefix);
        }
        return false;
    }

    /*
    @Description: 统计文件夹包含项数量时，可能会阻塞主线程，因此这里将它线程化。
    @Return: Future<Integer> 通过Future<Integer> 可尝试取消任务并获取实时计算信息
    @Protocol:
        关于返回对象Future<Integer> 的接口描述：
        > boolean cancel(boolean mayInterruptIfRunning)
            发出中断信号，并返回发出中断信号后，到函数结束时的取消结果，该方法未非阻塞方法，如果需要确切判断是否已经中断
            需要通过isCancelled() 进行重复检测
            调用该方法后，isDone()将返回true
        > isCancelled()
            判断取消任务是否成功，如果该方法返回true,则isDone()方法也会返回true
        > isDone()
            判断任务是否完成
        > Integer get()
            获取当前已统计的文件数目，需要注意的是，如果cancel() 被调用，则该方法返回值将可能
            就此稳定下来
        > Integer get(long l,TimeUnit timeUnit) throws InterruptException
            使当前线程等待一段时间后，再去获取统计值
            timeUnit 可设定多种值{
                    DAYS
                    时间单位代表二十四小时
                    HOURS
                    时间单位代表六十分钟
                    MICROSECONDS
                    时间单位代表千分之一毫秒
                    MILLISECONDS
                    时间单位为千分之一秒
                    MINUTES
                    时间单位代表60秒
                    NANOSECONDS
                    时间单位代表千分之一千分之一
                    SECONDS
                时间单位代表一秒
            }
            如果在等待过程中，这个线程被中断，get方法会抛出InterruptException,使用时应注意
            此异常的合理处理

    */
    public Future<Integer> getFileTotalCount(){
        FileCounterTask counterTask=new FileCounterTask(this);
        sharedThreadPool.executeTask(counterTask,SharedThreadPool.PRIORITY.LOW);
        return counterTask;
    }


    /*
    @Description: 获取文件夹下的及其子目录的所有总数,该方法为阻塞方法，不建议直接使用
     */
    public int _getFileTotalCount(){
        int count=0;
        if(isDirectory()){
            count=file.listFiles().length;
            FileHandle[] dirs=listFiles(new FileHandleFilter() {
                @Override
                public boolean accept(FileHandle handle) {
                    return handle.isDirectory();
                }
            });
            if(dirs!=null) {
                for (int i = 0; i < dirs.length; i++) {
                    count = count + dirs[i]._getFileTotalCount() - 1;
                }
            }
        }
        else{
            count=1;
        }
        return count;
    }
    /*

     */

    /*
    @Description: 该方法只是检测是否存在父目录，至于父目录
    是否可以写或者可读，无法清楚，因为FileHandle是相对于抽象路径名而言的
    如果需要确认该FileHandle是否是外部存储挂载中的内部存储卡目录或是sd卡根目录，需要使用
    isAndroidRoot()来查看
     */
    public boolean isRoot(){
        return (file.getParentFile()==null);
    }

    /*
    @Description: 通过字符串的形式检查该文件句柄所对应的抽象路径是否是可读写的根目录的其一
    @Notice: 只有当core.Service类初始化后此函数才发挥作用。
    @Return: 返回false的情况：1.不是可操作的根目录 2. 无法通过抽象路径的绝对路径名找到正则路径名
     */

    public boolean isInAndroidVolume(){
        return isInStorageVolume()||isInSDCardVolume();
    }



    public boolean isInStorageVolume(){
        return AbsolutePathName.startsWith(storage0_prefix);
    }


    /*
        @Description: 判断该文件句柄是否对应着sd卡根目录
     */

    public boolean isInSDCardVolume(){
        if(sdcard_prefix==null){
            return false;
        }
        return AbsolutePathName.startsWith(sdcard_prefix);
    }

    /*
    @Description: 返回目标所在的挂载点
    @Return: NotNull , 服务未初始化时返回空字符串
     */
    public String getVolumePrefix(){
        if(AbsolutePathName.startsWith(storage0_prefix)){
            return storage0_prefix;
        }
        else if(sdcard_prefix!=null){
            if(AbsolutePathName.startsWith(sdcard_prefix)) {
                return sdcard_prefix;
            }
            else {
                return "";
            }
        }
        else{
            return "/";
        }

    }
    //FileHandle封装下的可空文件句柄判断
    @Deprecated
    public boolean isNull(){
        return (file==null);
    }

    public boolean isExist(){
        return file.exists();
    }


    /*
    @Description: 创建一个以该路径命名的文件，如果该路径不存在，则
    返回值为false,即创建失败
     */
    public boolean create(){
        try {
            file.createNewFile();
            return true;
        }
        catch(IOException ex){
            Log.e("FileHandle: create",ex.getMessage());
            return false;
        }
    }

    /*
    @Description 不自动创建不包含的父目录，但在有时候能提高效率
    @Return: 不使用FileHandle检查常规的目录
     */
    public boolean makeDirectory(){
        return file.mkdir();
    }

    /*
    @Description: 创建文件夹时自动把父目录补上，但这很有可能操作失败，
    当SecurityManager.checkWrite()的结果为不可写时。
     */
    public boolean makeDirectories(){
        return file.mkdirs();
    }
    //重命名，删除, 属于敏感操作

    /*
    @Description:
    @Notice:修改文件名需要使用合法的字符和长度，为了效率，合法检测交由前端处理。
    应该总是注意检查该函数的返回结果来确认改名是否成功。
    @Return: 如果返回false 则说明存在同名文件，或者文件受保护。
     */
    synchronized public boolean rename(String newName){
        String NewFileName= getParentPathNameByAbsolutePathName();
        NewFileName=NewFileName.concat("/"+newName);
        File renamed_file=new File(NewFileName);
        if(renamed_file.exists()){
            return false;
        }
        return file.renameTo(renamed_file);
    }

    //删除文件，如果这是非空文件夹，则会返回false
    //目标文件不存在或无法删除都将返回false,可能会抛出异常

//    synchronized boolean delete() throws IOException{
//        //delete方法的实现受API限制
//
//        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
//            try{
//                Files.delete(file.toPath());
//                return true;
//            }
//            catch(NoSuchFileException ex){
//                android.util.Log.e("[FileHandle delete]","no such file exist");
//                throw ex; //向上抛出以通知无法删除的原因。
//            }
//            catch(DirectoryNotEmptyException ex){
//                android.util.Log.e("[FileHandle delete]","Directory isn't empty");
//                throw ex;
//            }
//            catch(SecurityException ex){
//                android.util.Log.e("[FileHandle delete]","file is under protected by security manager");
//                throw ex;
//            }
//        }
//        else{
//            try {
//                return file.delete();
//            }
//            catch(SecurityException ex){
//                android.util.Log.e("[FileHandle delete]","file is under protected by security manager");
//                throw ex;
//            }
//        }
//
//    }

    /*
    @Description: 删除该FileHandle所对应的文件。如果目标是文件夹，且内部不为空，则
    操作失败
     */
    synchronized boolean delete(){
        return file.delete();
    }

    /*
    @Description: 递归删除文件夹,需要为其指定一个进度监视器,具体进度监视进度协议见下
    @Protocol: 监视器接口函数的生命周期调用：
    onStart()->[receiveMessage(message:String)->onStop(FAILED)]
    ->
    {
    onSubTaskStart(taskId)->onSubProgress(taskid:int,pathname:String,result_of_delete:boolean)
    ->[onSubTaskStop(PROGRESS_STATUS.FAILED)]->onSubTaskFinish(taskId)
    }*
    ->
    [onStop(PROGRESS_STATUS.FAILED)]->onFinish()
    简要的说明，每次删除每个空文件夹或单个文件时，都会调用onSubProgress()通告该文件或空文件夹的删除结果,
    当递归过程结束后，会根据删除文件的结果，如果删除不完全，则调用onStop()通报该任务为Failed，失败状态。
    不管任务是否失败，都会调用onFinish()标志该任务线程已结束。
    @Exception: 检测到该文件句柄所代表抽象路径含有文件名的非法字符时，会调用monitor 的onStop方法通告
    其删除线程当前的状态，并向monitor发送错误信息。
     */
    synchronized public void deleteRecursively(ProgressMonitor<String,Boolean> monitor){
        if(!FileHandle.containsIllegalChar(getAbsolutePathName())&&this.isExist()) {
            FileDeleteSingleThreadTask task = new FileDeleteSingleThreadTask(monitor, this);
            sharedThreadPool.executeTask(task,SharedThreadPool.PRIORITY.MEDIUM);
        }
        else{
            //deal with exception
            monitor.receiveMessage("this file handle contains illegal char and cannot be denoted to a specific file");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
        }
    }

    /*
    @Description:　阻塞方法的递归删除,null
     */
    synchronized public boolean _deleteRecursively(boolean[] stop){
        boolean delete_result=true;
        if(stop==null||stop[0]) {
            if (this.isDirectory()) {
                FileHandle[] list = this.listFiles();
                for (int i = 0; i < list.length; i++) {
                    delete_result = delete_result && list[i]._deleteRecursively(stop);
                }

                //delete file under folder first, then delete folder itself
                if (delete_result) {
                    delete_result = this.delete();
                }

            } else {
                delete_result = this.delete();
            }
            return delete_result;
        }
        return false;
    }


    /*
    @Description: 如果该文件句柄是目录，则返回一个FileHandle数组,
    如果遇到IO 错误或者该文件句柄不代表目录，则返回null
     */
    public FileHandle[] listFiles() {
        if(file.isDirectory()){
            File[] list=file.listFiles();
            if(list==null){
                return new FileHandle[0];
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

    public FileHandle[] listFiles(final FileHandleFilter filter){
        if(file.isDirectory()){
            File[] list=file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    FileHandle handle=new FileHandle(f);
                    return filter.accept(handle);
                }
            });
            if(list==null){
                return null;
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

    public boolean isDenotedToSameFile(FileHandle fileHandle){
        if(fileHandle==null){
            return false;
        }
        return this.getAbsolutePathName().contentEquals(fileHandle.getAbsolutePathName());
    }


    //元数据metadata 的操作
    /*
    可以使用textFormatter进行转换
     */
    public long getLastModifiedTime(){
        return file.lastModified();
    }

    /*
    平台差异，暂时不考虑ctime, atime
     */
//    public long getCreateTime(){
//        return
//    }
    @Override
    public FileHandle clone(){
        FileHandle CloneHandle=new FileHandle(this.file);
        return CloneHandle;
    }

    @Override
    public String toString(){
       return getName();
    }
    //Private methods


    //-----------------static methods---------------------------

    public static FileHandle superHandle=new FileHandle("/storage/super"); //a special handl
    private static String storage0_prefix="/storage/emulated/0";
    private static String storage_prefix="/storage";
    private static String sdcard_prefix=null;
    private static SharedThreadPool sharedThreadPool=SharedThreadPool.getInstance();

    /*
    @Description： 检查该FileHandle所对应的抽象路径名是否含有非法字符
     */
    public static boolean containsIllegalChar(String args){
        String regex="[\\\\/:*?\"<>|]*";
        boolean match=args.matches(regex);
        return match;
    }

    public static void _initPrefixName(Service svc){
        sdcard_prefix=svc.getSDCardRootDirectoryPathName();
    }

    static public boolean makeDirectory(FileHandle parent, String name){
        FileHandle folderToBeCreated=new FileHandle(parent.getAbsolutePathName().concat("/").concat(name));
        return folderToBeCreated.makeDirectory();
    }
    //---------------分割线--------------------------------


    //private helper function---------------------------------


    //private helper class

    private class FileCounterTask implements RunnableFuture<Integer> {

        private FileHandle handle;

        private int count=0;
        private boolean cancelSignal=false;
        private boolean cancelled=false;
        private boolean done=false;


        public FileCounterTask(FileHandle arg_handle){

            handle=arg_handle;
            count=0;
        }

        @Override
        public void run() {
            FilesCountHelperFunction(handle);
            done=true;
        }

        private void FilesCountHelperFunction(FileHandle dir){
            if(!cancelSignal) {
                FileHandle[] files_of_dir = dir.listFiles();
                int this_dir_count = files_of_dir.length;
                if (files_of_dir != null) {
                    FileHandle[] folders_of_dir = dir.listFiles(new FileHandleFilter() {
                        @Override
                        public boolean accept(FileHandle handle) {
                            return handle.isDirectory();
                        }
                    });
                    this_dir_count -= folders_of_dir.length;
                    for (int i = 0; i < folders_of_dir.length; i++) {
                        FilesCountHelperFunction(folders_of_dir[i]);
                    }
                }
                count += this_dir_count;
            }
            else if(!cancelled){
                cancelled=true;
                done=true;
            }
        }

        /*
        注意
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelSignal=mayInterruptIfRunning;
            done=true;
            return cancelled;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }


        @Override
        public Integer get() {
            return count;
        }
        /*
        如果需要等待最多在给定的时间计算完成，然后检索其结果（如果可用）。
        参数
        timeout - 等待的最长时间
        unit - 超时参数的时间单位
        结果:
           统计结果
        异常:
           InterruptedException - 如果当前线程在等待时中断


         */
        @Override
        public Integer get(long l, TimeUnit timeUnit)throws InterruptedException{
            l=timeUnit.toMillis(l);
            Thread.sleep(l);
            return count;
        }
    }

    /*
    监视器方法在此处已经弃用，比起频繁地通告，主动获取的效率会更高
     */
    private class FileSizeCounterTask implements RunnableFuture<Long>{

        private FileHandle rootHandle=null;
        private long sizeAccumulated=0;  //no need to synchronize because this version of SizeCounter is blocking
        private boolean hasSetStopBit=false;
        private boolean cancelSignal=false;
        private boolean cancelled=false;
        private boolean done=false;

        public FileSizeCounterTask(FileHandle fileHandle){
            rootHandle=fileHandle;
        }
        @Override
        public void run() {
            totalSizeHelperFunction(rootHandle);
            done=true;
        }

        private void totalSizeHelperFunction(FileHandle handle)  {
            if(!cancelSignal) {
            FileHandle[] list_item = handle.listFiles();
            sizeAccumulated += handle.Size();
                if (list_item != null) { //list_item is not null directory
                    for (int i = 0; i < list_item.length; i++) {
                        totalSizeHelperFunction(list_item[i]); //add cumulatively recursively
                    }
                }
            }
            else if(!cancelled){
                cancelled=true;
                done=true;
            }

        }


        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelSignal=mayInterruptIfRunning;
            done=true;
            return cancelled;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public Long get() {
            return sizeAccumulated;
        }

        @Override
        public Long get(long l, TimeUnit timeUnit) throws  InterruptedException {
            l=timeUnit.toMillis(l);
            java.lang.Thread.sleep(l);
            return sizeAccumulated;
        }
    }

    private class FileDeleteSingleThreadTask implements Runnable{

        private FileHandle node;
        private ProgressMonitor<String,Boolean> monitor;
        private boolean delete_result;
        private boolean hasSetStopBit=false;
        private int subTaskId=0;

        public FileDeleteSingleThreadTask(ProgressMonitor<String,Boolean> arg_monitor,FileHandle startNode){
            node=startNode;
            monitor=arg_monitor;
        }

        @Override
        public void run() {
            monitor.onStart();
            deleteRecursivelyHelper(node);
            monitor.onProgress(node.getAbsolutePathName(),delete_result);
            if(!delete_result){
                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                return;
            }
            monitor.onFinished();

        }

        synchronized boolean deleteRecursivelyHelper(FileHandle handle){
            if(!monitor.abortSignal()) {
                waitUntilFalse();
                FileHandle[] handles = handle.listFiles();
                //handle is a file or an empty folder
                if (handles == null) {
                    monitor.onSubTaskStart(subTaskId);
                    boolean result = handle.delete();
                    monitor.onSubProgress(subTaskId,handle.getAbsolutePathName(), result);
                    if(!result){
                        monitor.onSubTaskStop(subTaskId, ProgressMonitor.PROGRESS_STATUS.FAILED);
                    }
                    monitor.onSubTaskFinish(subTaskId);
                    subTaskId++;

                    delete_result = delete_result && result;
                    return result;
                }

                boolean folder_delete_result = true;
                //delete files under folder first
                for (int i = 0; i < handles.length; i++) {
                    folder_delete_result = folder_delete_result && deleteRecursivelyHelper(handles[i]);
                }

                //then delete folder
                if(folder_delete_result){
                    monitor.onSubTaskStart(subTaskId);
                    //borrow the variable "folder_delete_result" for temporary usage
                    folder_delete_result=handle.delete();
                    if(folder_delete_result){
                        monitor.onSubTaskStop(subTaskId,ProgressMonitor.PROGRESS_STATUS.FAILED);
                    }
                }
                else{
                    monitor.onSubTaskStop(subTaskId, ProgressMonitor.PROGRESS_STATUS.FAILED);
                }
                monitor.onSubTaskFinish(subTaskId);
                subTaskId++;

                //redundant set operation but useful
                delete_result = delete_result && folder_delete_result;
                return folder_delete_result;
            }
            else if(!hasSetStopBit){
                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.ABORTED); //确保这个onStop只执行一次
                return false;
            }
            return false;
        }

        private void waitUntilFalse(){
            while(monitor.interruptSignal()){
                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.PAUSED);
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    return;
                }

            }
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.GOING);
        }
    }

}
