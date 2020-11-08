package com.scut.filemanager.core;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
            CanonicalPathName=dir.getCanonicalPathName()+pathname; //notice this variable of how it comes
        }
        else{
            file=dir.file;
            AbsolutePathName=dir.getAbsolutePathName();
            CanonicalPathName=dir.getCanonicalPathName();
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
    统计文件夹包含项大小时，可能会阻塞主线程，因此这里将它多线程化。
    启动该函数时，会另起一个线程，通知监控器统计的结果，这里暂时返回空值
    返回该文件总大小，如果是目录则计量目录下的文件大小
    该方法的实现需要确保线程安全,为了确保该函数有返回结果，需要阶段性地反馈信息
    如果该函数统计超时，则需要通知父线程，即监控器所在线程该消息。
    */
    public void totalSize(ProgressMonitor<String,Long> monitor) throws IOException {
        FileSizeCounterTask fileSizeCounter=new FileSizeCounterTask(monitor,this );
        Thread thread_SizeCounter=new Thread(fileSizeCounter);
        thread_SizeCounter.start();
    };

    /*
    @Description: 该方法用于尝试获取一次正则路径。
     */
    public void tryRetrieveCanonicalPath(){
        try{
            CanonicalPathName=file.getCanonicalPath();
        }
        catch(IOException ioex){
            CanonicalPathName=null;
            Log.e("FileHandle","tryRetrieveCanonicalPath failed");
        }
    }



    /*
    @Description: 获取文件当前所在路径正则路径名,需要注意的是，有时在获取正则路径名时会
    遭遇失败，比如因为在查询CanonicalPath是需要进行文件系统的查询工作
    @Nullable
     */
    public String getCanonicalPathName()  {
        return CanonicalPathName;
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
    public String getParentName(){
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

    /*
    @Description： 获取文件夹下及其子目录的所有总数,该方法为非阻塞方法，但需要设定进度监控器。
    进度监控器会被定期汇报每个文件夹项的文件总数，因此使用到的进度的汇报方式时，需要将
    整个onProgress设定为临界区。
     */
    public void getFileTotalCount(ProgressMonitor<String,Integer> monitor){
        monitor.onStart();
        ExecutorService executor=Executors.newFixedThreadPool(2);
        FileHandle[] dirs=listFiles(new FileHandleFilter() {
            @Override
            public boolean accept(FileHandle handle) {
                return handle.isDirectory();
            }
        });
        int count=file.listFiles().length;
        if(dirs!=null){
            count-=dirs.length;
            for(int i=0;i<dirs.length;i++){
                FileCounterTask counter=new FileCounterTask(monitor,dirs[i]);
                executor.execute(counter);
            }
        }
        else{
            monitor.onProgress(getName(),count);
            monitor.onFinished();
        }
    }


    /*
    @Description: 获取文件夹下的及其子目录的所有总数,该方法为阻塞方法，不建议直接使用
     */
    public int getFileTotalCount(){
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
                    count = count + dirs[i].getFileTotalCount() - 1;
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
    @Description: 通过字符串的形式检查该文件句柄所对应的抽象路径是否已经到达可读写的根目录
    @Notice： 需要注意的是，为了解耦，一般情况下，filehandle 并不知道系统的内部存储卡和sd卡等的
    @Return: 返回false的情况：1.不是可操作的根目录 2. 无法通过抽象路径的绝对路径名找到正则路径名
     */
    public boolean isAndroidRoot(){
        return isStorageRoot()||isSDcardRoot();
    }



    public boolean isStorageRoot(){
        if(CanonicalPathName!=null){
            return CanonicalPathName.equals(FileHandle.storage0_prefix);
        }
        else{
            return false;
        }
    }


    /*
        @Description: 判断该文件句柄是否对应着sd卡根目录
     */
    public boolean isSDcardRoot(){
        if(CanonicalPathName!=null){
            //在不清楚sd卡的型号下，只能暂时通过排除法来判断
            String regex="/storage/[^/]+";
            boolean match=CanonicalPathName.matches(regex);
            return match&&canRead(null);
        }
        else{
            return false;
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
    synchronized boolean rename(String newName) throws IOException {
        String NewFileName=getParentName();
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
    @Description: 递归删除文件夹,需要为其指定一个进度监视器,在第一版中，暂时将线程分散管理。
    @Exception: 检测到该文件句柄所代表抽象路径含有文件名的非法字符时，会调用monitor 的onStop方法通告
    其删除线程当前的状态，并向monitor发送错误信息。
     */
    synchronized public void deleteRecursively(ProgressMonitor<String,Boolean> monitor){
        if(!FileHandle.containsIllegalChar(getAbsolutePathName())) {
            FileDeleteSingleThreadTask task = new FileDeleteSingleThreadTask(monitor, this);
            Thread deleteThread = new Thread(task);
            deleteThread.start();
        }
        else{
            //deal with exception
            monitor.receiveMessage("this file handle contains illegal char and cannot be denoted to a specific file");
            monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
        }
    }


    //如果该句柄是文件，则返回空，否则返回目录下的项目句柄s。
    //获取目录下的项目句柄
    public FileHandle[] listFiles() {
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
    //Private methods


    //-----------------static methods---------------------------

    private static String storage0_prefix="/storage/emulated/0";
    private static String storage_prefix="/storage";


    /*
    @Description： 检查该FileHandle所对应的抽象路径名是否含有非法字符
     */
    public static boolean containsIllegalChar(String args){
        String regex="[\\\\/:*?\"<>|]*";
        boolean match=args.matches(regex);
        return match;
    }

    //---------------分割线--------------------------------


    //private helper function---------------------------------


    //private helper class

    private class FileCounterTask implements Runnable{

        private FileHandle handle;
        private ProgressMonitor<String,Integer> monitor;

        public FileCounterTask(ProgressMonitor<String,Integer> arg_monitor, FileHandle arg_handle){
            monitor=arg_monitor;
            handle=arg_handle;
        }

        @Override
        public void run() {
            if(monitor.abortSignal()){
                int count = handle.getFileTotalCount();
                monitor.onProgress(handle.getName(),Integer.valueOf(count));
            }
        }

    }

    private class FileSizeCounterTask implements Runnable{

        private FileHandle rootHandle=null;
        private long sizeAccumulated=0;  //no need to synchronize because this version of SizeCounter is blocking
        private ProgressMonitor<String,Long> monitor;
        public long getSizeAccumulated(){
            return sizeAccumulated;
        }

        public FileSizeCounterTask(ProgressMonitor<String,Long> arg_monitor, FileHandle fileHandle){
            monitor=arg_monitor;
            rootHandle=fileHandle;
        }
        @Override
        public void run() {
            monitor.onStart();
            totalSizeHelperFunction(rootHandle);
            monitor.onFinished();
        }

        private void totalSizeHelperFunction(FileHandle handle)  {
            FileHandle[] list_item = handle.listFiles();
            sizeAccumulated += handle.Size();
            monitor.onProgress("sizeAccumulated",sizeAccumulated);
            if(!monitor.abortSignal()) {
                if (list_item != null) { //list_item is not null directory
                    for (int i = 0; i < list_item.length; i++) {
                        totalSizeHelperFunction(list_item[i]); //add cumulatively recursively
                    }
                }
            }
        }


    }

    private class FileDeleteSingleThreadTask implements Runnable{

        private FileHandle node;
        private ProgressMonitor<String,Boolean> monitor;
        private boolean delete_result;
        private boolean interrupt_deletion=false;

        public FileDeleteSingleThreadTask(ProgressMonitor<String,Boolean> arg_monitor,FileHandle startNode){
            node=startNode;
            monitor=arg_monitor;
        }

        @Override
        public void run() {
            monitor.onStart();
            deleteRecursively(node);
            monitor.onProgress(node.getAbsolutePathName(),delete_result);
            if(!interrupt_deletion) {
                monitor.onFinished();
            }
        }

        synchronized boolean deleteRecursively(FileHandle handle){
            if(!monitor.abortSignal()) {
                FileHandle[] handles = handle.listFiles();
                //handle is a file or an empty folder
                if (handles == null) {
                    boolean result = handle.delete();
                    monitor.onProgress(handle.getAbsolutePathName(), result);
                    delete_result = delete_result && result;
                    return result;
                }

                boolean folder_delete_result = true;
                for (int i = 0; i < handles.length; i++) {
                    folder_delete_result = folder_delete_result && deleteRecursively(handles[i]);
                    folder_delete_result = folder_delete_result && handle.delete();
                    monitor.onProgress(handle.getAbsolutePathName(), folder_delete_result);
                }
                delete_result = delete_result && folder_delete_result;
                return folder_delete_result;
            }
            else{
                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.ABORTED);
                interrupt_deletion=true;
                return false;
            }
        }
    }

}
