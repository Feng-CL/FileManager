package com.scut.filemanager.core.net;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.scut.filemanager.DeviceSelectActivity;
import com.scut.filemanager.FMGlobal;
import com.scut.filemanager.MainController;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.ProgressMonitor;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.concurrent.SharedThreadPool;
import com.scut.filemanager.core.internal.BoardCastScanWatcher;
import com.scut.filemanager.ui.adapter.DeviceListViewAdapter;
import com.scut.filemanager.ui.transaction.Request;
import com.scut.filemanager.util.FMFormatter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

//该类是启动和控制服务的起点，
public class NetService extends BoardCastScanWatcher {

    //网络适配器
    private WifiManager wifi;
    private WifiInfo wifiInfo=null;
    private Enumeration<NetworkInterface> netInterfaces;

    //dependent filemanager.core.service
    private Service service;

    //save status
    private NetStatus wifi_status =NetStatus.UNINITIALIZED;
    private boolean isScanning=false;


    //static members
    private static NetService netService=null;
    //

    //UI Activity:
    private DeviceSelectActivity deviceSelectActivity =null;

    //function members
    private OnlineBoardCaster caster;
    public BoardCastScanner scanner;
    private FileReceiverClient server;
    private ListenerAcceptLoop connectionAcceptLooper=null;

    //reference controller
    private MainController main_controller=null;

    //Threads
    Thread[] threads=new Thread[2];



    public static NetService getInstance(Service service){
        if(netService==null&&service!=null){
            netService=new NetService(service);
        }
        return netService;
    }

    private NetService(@NonNull Service core_service) {

        this.service=core_service;
        //首先检查WiFi权限和访问状态
        wifi=(WifiManager)(service.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE));

        //无法获取WiFi 服务,归咎于权限
        if(wifi==null){
            wifi_status =NetStatus.WIFI_PERMISSION_DENY;
        }
        else {

            int wifiStatusi=wifi.getWifiState();
            switch(wifiStatusi){
                case WifiManager.WIFI_STATE_DISABLED:
                case WifiManager.WIFI_STATE_DISABLING:
                    wifi_status =NetStatus.WIFI_DISABLED;
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                case WifiManager.WIFI_STATE_ENABLED:
                    wifi_status =NetStatus.WIFI_ENABLED;
                    break;
                default:
                    wifi_status =NetStatus.WIFI_STATUS_UNKNOWN;
            }

            if(wifi_status ==NetStatus.WIFI_ENABLED){
                wifiInfo=wifi.getConnectionInfo();
                if(wifiInfo.getIpAddress()!=-1){
                    //wifi enabled but not connect or getIpAddress
                    wifi_status =NetStatus.WIFI_CONNECTED;
                }
                //获取网卡信息
//                try {
//                    netInterfaces = NetworkInterface.getNetworkInterfaces();
//                } catch (SocketException sockEx) {
//                    sockEx.printStackTrace();
//                }
//                while(netInterfaces.hasMoreElements()){
//                    NetworkInterface net_interface=netInterfaces.nextElement();
//                    Enumeration<InetAddress> ips=net_interface.getInetAddresses();
//                    while(ips.hasMoreElements()){
//                        InetAddress inetAddress=ips.nextElement();
//                        Log.i("test NetService:","hostname: "+inetAddress.getHostAddress()+" canonical hostname: " +
//                                inetAddress.getCanonicalHostName()+" hostAddress"+inetAddress.getHostAddress());
//                    }
//                }
            }

        }


    }


    /**
     * 如果wifi未启用并连接，则会提示用户手动去连接，暂时不拉起相关设置页面
     */
    public void startScanner() {

        if(getWifi_status()== NetStatus.WIFI_CONNECTED) {

            if (!isScanning) {
                try {
                    DatagramSocket udpSocket = new DatagramSocket(33720); //33720 listen
                    this.scanner = new BoardCastScanner(udpSocket, this);
                    threads[0] = new Thread(this.scanner);
                    threads[0].start();
                } catch (SocketException socket_exception) {
                    if (this.deviceSelectActivity != null) {
                        Message errMsg = Message.obtain();
                        errMsg.what = DeviceSelectActivity.UIMessageCode.UPDATE_ERR_MSG;
                        errMsg.obj = socket_exception.getMessage();
                        this.deviceSelectActivity.mHandler.sendMessage(errMsg);
                    }
                    return;
                }
                isScanning = true;
            }
        }
        else{
            //notify user or report it to upper handler
        }
    }

    public void startBoardCaster() throws SocketException {
        this.caster=OnlineBoardCaster.getInstance(this);
        threads[1]=new Thread(this.caster);
        threads[1].start();
    }

    public NetStatus getWifi_status(){
        return wifi_status;
    }

    public WifiInfo getWifiConnectionInfo(){
        return wifiInfo;
    }

    public InetAddress getWifiIp4Address() {
        String IPstr=FMFormatter.ip4Address_i2s(wifiInfo.getIpAddress());
        try {
            return InetAddress.getByName(IPstr);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Protocols:
     * 监视器需要根据receiveMessage的结果来进行相应的显示输出，因为
     * 这里的receiveMessage将会混合了错误码一同被调用，但需要注意的是
     * 目前错误码的receiveMessage将会只调用一次，结果之后将是onStop(#ProgressMonitor:Status)
     * 任务开始后，将会像目标发送元信息，如果等待超时，任务将会自动退出，
     * 并以PROGRESS_STATUS.FAILED 状态结束，如果对方拒绝，监视器将会收到
     * NetService.MessageCode.NOTICE_CONNECTION_DECLINED的消息码
     * 任务同样也会结束
     * 对方同意后，任务自动继续，并相继向监视器发送消息码
     * NetService.MessageCode.NOTICE_CONNECTING
     * NetService.MessageCode.NOTICE_CONNECTED 表示连接的状态
     * 如果TCP连接建立成功，随后将会调用onProgress汇报总体大小，通过
     * onSubProgress(null,FilePathName,size)汇报当前发送中的文件和当前以发送的总
     * 字节数。
     * 一切ok将调用onFinished()否则将会以onStop()结束,结束的原因可能是对方中断了连接或者其他异常出现。
     */
    public void send(InetAddress targetAddress, List<FileHandle> listOfFiles,ProgressMonitor<String,Long> monitor){
        if(listOfFiles.size()>0){
            //monitor.onStart();
            SendFilesTask task=new SendFilesTask(targetAddress,listOfFiles,monitor);
            SharedThreadPool.getInstance().executeTask(task,SharedThreadPool.PRIORITY.HIGH);
        }
    }

    public void receive(InquirePacket inquirePacket,ProgressMonitor<String,Long> monitor){
        FileReceiverClient client = new FileReceiverClient(inquirePacket.ip,(FileNodeWrapper)inquirePacket.obj,monitor);
        client.startClient();
    }

    public void refuseAndSendNACK(InetAddress target){
        ACKTask refuseTask=new ACKTask(target,false);
        SharedThreadPool.getInstance().executeTask(refuseTask,SharedThreadPool.PRIORITY.MEDIUM);
    }

    public void acceptAndSendACK(InetAddress target){
        ACKTask acceptTask=new ACKTask(target,true);
        SharedThreadPool.getInstance().executeTask(acceptTask,SharedThreadPool.PRIORITY.HIGH);
    }



    /*
    @Description: return null if netServiceStatus isn't WIFI_CONNECTED
     */
    public String getWifiIpAddressDesc(){
        if(getWifi_status()==NetStatus.WIFI_CONNECTED){
            return FMFormatter.ip4Address_i2s(wifiInfo.getIpAddress());
        }
        return null;
    }


    /*
        @Description:获取手机型号
        @Params:
    */
    public static String getDeviceModel(){
        StringBuilder str_builder=new StringBuilder(android.os.Build.MODEL.intern());
        return str_builder.toString();
    }



    /**
     * 这里的函数应该是处理已经过滤过的InquirePacket,只有这样才能避免多次刷新界面
     * @param key
     * @param value
     */
    @Override
    public void onProgress(InetAddress key, InquirePacket value) {

        switch (value.what) {
            case InquirePacket.MessageCode.IP_NULL:
                if(deviceSelectActivity!=null){
                    //id 为ip的hashcode
                    DeviceListViewAdapter.ItemData itemData = new DeviceListViewAdapter.ItemData(key.hashCode());
                    itemData.DeviceIp = key.getHostAddress();
                    itemData.DeviceName = value.description;
                    deviceSelectActivity.mHandler.sendMessage(
                            Request.obtain(DeviceSelectActivity.UIMessageCode.NOTIFY_DATASET_CHANGE, itemData)
                    );
                }
                break;
            case InquirePacket.MessageCode.IP_FILES_AND_FOLDERS:
                //invoke ui dialog
                main_controller.InvokeReceiveInquireDialog(value);
                break;
            default:
                this.pushInquirePacket(value);
                break;
        }
    }

    enum NetStatus{
        WIFI_CONNECTED, //wifi 已连接并获得IP地址
        LTE_CONNECTED,  //这个凑数
        WIFI_DISABLED, //wifi 被关闭
        WIFI_ENABLED,   //wifi 被打开，但是没有连接
        WIFI_PERMISSION_DENY, //wifi 无权限访问
        WIFI_STATUS_UNKNOWN,//universal status
        UNINITIALIZED
    }

    public void stopScanning(){
        isScanning=false;
        scanner.stop();
    }

    /*
        @Description:由Net来完成双向绑定
        @Params:
    */

    public void bindDeviceSelectActivity(@NonNull DeviceSelectActivity activity){
        this.deviceSelectActivity =activity;
        this.deviceSelectActivity.setNetServiceRef(this);
    }

    public void setMainController(MainController controller){
        main_controller=controller;
    }

    public void unBindDeviceSelectActivity(){
        this.deviceSelectActivity =null;
    }

    public boolean isScanning(){
        return this.isScanning;
    }

    /**
     * 通知绑定的Activity以MessageEntry的形式
     */
    public void notifyActivityToToast(String text){
        if(this.deviceSelectActivity !=null){
            this.deviceSelectActivity.mHandler.sendMessage(
                    Request.obtain(FMGlobal.MAKE_TOAST,text)
            );
        }
    }


    /**
     * 该内部类相当于一个代理了构造询问包，建立连接的一个类，它独立于一个线程中工作，并且需要为它传入
     * 监视器对象，至于监视器对象如何通知前端进行显示，需要由监视器对象根据取得的信息进行操作。
     */

    private static class InnerShare {
        static short udpSocketSemaphore=0; //关闭udpSocket时的检查的共享信号，也可用来指定
        static DatagramSocket udpSocket=null;
    }

    private class ACKTask implements Runnable{

        private InetAddress target;
        private boolean ack;

        ACKTask(InetAddress target,boolean ack){
            this.target=target;
            this.ack=ack;
        }

        @Override
        public void run() {
            try{
                InnerShare.udpSocketSemaphore++;
                if(InnerShare.udpSocket==null) {
                    InnerShare.udpSocket = new DatagramSocket();
                }
                InquirePacket inquirePacket;
                if(!ack) {
                    inquirePacket = new InquirePacket(InquirePacket.MessageCode.N_ACK_IP_FILES_AND_FOLDER);
                }
                else{
                    inquirePacket=new InquirePacket(InquirePacket.MessageCode.ACK_IP_FILES_AND_FOLDERS);
                }
                byte[] buf=inquirePacket.getBytes();
                DatagramPacket pkt=new DatagramPacket(buf,buf.length,target,FMGlobal.BoardCastReceivePort);
                InnerShare.udpSocket.send(pkt);
                Log.d("ACKTask","send ACK");
            }catch (IOException e) {
                Log.e("RefuseTask",e.getMessage());
            }finally {
                InnerShare.udpSocketSemaphore--;
                synchronized (InnerShare.udpSocket) {
                    if (InnerShare.udpSocketSemaphore == 0) {
                        InnerShare.udpSocket.close();
                    }
                }
            }
        }
    }

    private class SendFilesTask implements Runnable{

        ProgressMonitor<String,Long> monitor; //key为传输中的对象路径名称,Long 为传输中的总字节，暂定
        List<FileHandle> listOfFiles;
        InetAddress targetAddress;


        public SendFilesTask(InetAddress address, List<FileHandle> fileHandles, ProgressMonitor<String,Long> monitor){
            this.monitor=monitor;
            this.listOfFiles=fileHandles;
            targetAddress=address;
        }

        @Override
        public void run(){
            try {
                //这个udpSocket 可以被复用，不必在每个子线程中多次创建。不过需要特别地管理其生命周期，一般地
                //总是在最后一个线程使用完后释放。
                monitor.onStart();
                this.registerSharedUDPSocket();
                if(InnerShare.udpSocket==null) {
                    InnerShare.udpSocket = new DatagramSocket();
                    InnerShare.udpSocket.setSoTimeout(60 * 1000);
                }

                InquirePacket inquirePacket=prepareInquirePacket(listOfFiles);
                try {
                    byte[] bytesOfInquirePacket=inquirePacket.getBytes();
                    DatagramPacket packet=new DatagramPacket(bytesOfInquirePacket,bytesOfInquirePacket.length,targetAddress,33720);

                    InnerShare.udpSocket.send(packet);
                    //waiting
                    short respond=waitForACK(targetAddress);
                    while(respond==0){
                        //sleeping
                        try{
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            monitor.receiveMessage(MessageCode.ERR_INTERRUPT_EXCEPTION,e.getMessage() );
                            monitor.onStop(PROGRESS_STATUS.FAILED);
                            this.attemptToCloseUDPSocket();
                            return;
                        }
                        respond=waitForACK(targetAddress);  //此处容易造成无限等待，应该设置超时定时器
                    }
                    //check whether opposite deny the connect request
                    if(respond==-1){
                        monitor.receiveMessage(MessageCode.NOTICE_CONNECT_DECLINED,"connect request is declined");
                        monitor.onStop(PROGRESS_STATUS.ABORTED);
                        this.attemptToCloseUDPSocket();
                        return;
                    }

                    //prepare to build up connection, respond=1;
                    monitor.receiveMessage(MessageCode.NOTICE_CONNECTING,null);
                    Socket socket= procedure_connect_to_target();
                    if(socket!=null){
                        monitor.receiveMessage(MessageCode.NOTICE_CONNECTED,null);
                        if(inquirePacket.obj instanceof FileNodeWrapper){
                            FileNodeWrapper wrapper= (FileNodeWrapper) inquirePacket.obj;
                            boolean process_result=procedure_transmission(socket.getOutputStream(),wrapper);
                            if(!process_result){
                                this.attemptToCloseUDPSocket();
                                socket.close();
                                return;
                            }
                        }
                        else{
                            //report error
                            monitor.receiveMessage(MessageCode.ERR_UNKNOWN,"FileNodeWrapper downcast error");
                            monitor.onStop(PROGRESS_STATUS.FAILED);
                            socket.close();
                            return;
                        }
                        socket.getOutputStream().flush();
                        socket.close();
                        Log.d("sendFilesTask", "run: on Finished");
                        monitor.onFinished();  //Finish point
                    }


                } catch (IOException e) {
                    this.attemptToCloseUDPSocket();
                    monitor.receiveMessage(MessageCode.ERR_IO_EXCEPTION,e.getMessage());
                    monitor.onStop(PROGRESS_STATUS.FAILED);
                } catch (InterruptedException e) {
                    this.attemptToCloseUDPSocket();
                    monitor.receiveMessage(MessageCode.ERR_INTERRUPT_EXCEPTION,e.getMessage());
                    monitor.onStop(PROGRESS_STATUS.FAILED);
                }
            } catch (SocketException e) {
                monitor.receiveMessage(MessageCode.ERR_SOCKET_EXCEPTION,e.getMessage() );
                monitor.onStop(PROGRESS_STATUS.FAILED);
            }
        }

        synchronized private void attemptToCloseUDPSocket(){
            InnerShare.udpSocketSemaphore--;
            if(InnerShare.udpSocketSemaphore<=0){
                InnerShare.udpSocket.close();
            }
        }

        private void registerSharedUDPSocket(){
            InnerShare.udpSocketSemaphore++;
        }


        private InquirePacket prepareInquirePacket(@NonNull List<FileHandle> list){
            //temporary create a virtual parent folder to wrap the files in list
            InquirePacket inquirePacket=new InquirePacket(InquirePacket.MessageCode.IP_FILES_AND_FOLDERS);
            FileNode root=FileNode.createNodeFromList("wrap",0,list,null);
            FileNodeWrapper nodeWrapper =new FileNodeWrapper(root,true);

            //这里视list中的FileHandle为同一目录下的文件集，因此取第一个获取父目录的路径
            nodeWrapper.setRootPath(list.get(0).getParentPathNameByAbsolutePathName());
            inquirePacket.obj=nodeWrapper;

            return inquirePacket;
        }


        private short waitForACK(InetAddress ip){
            Queue<InquirePacket> queue= NetService.this.cacheTable.get(ip);
            InquirePacket inquirePacketFromIp=null;
            if(queue!=null) {
                Log.d("WaitForACK","queue is NonNull");
                inquirePacketFromIp = queue.poll();
            }
            if(inquirePacketFromIp==null){
                Log.d("WaitForACK","poll() is null");
                return 0;
            }
            else {
                if (inquirePacketFromIp.what == InquirePacket.MessageCode.ACK_IP_FILES_AND_FOLDERS) {
                    Log.d("WaitForACK","return 1");
                    return 1;
                } else if (inquirePacketFromIp.what == InquirePacket.MessageCode.N_ACK_IP_FILES_AND_FOLDER) {
                    return -1;
                } else {
                    Log.d("WaitForACK","return 0");
                    return 0;
                }
            }
        }

        /**
         * 该函数与connectionAcceptLooper所在线程交互，服务器监听线程需要与传输任务的 线程进行异步
         * @return
         */
        private Socket procedure_connect_to_target() throws InterruptedException {
            if (connectionAcceptLooper == null) {
                connectionAcceptLooper = new ListenerAcceptLoop(monitor, InnerShare.udpSocketSemaphore);
                SharedThreadPool.getInstance().executeTask(connectionAcceptLooper, SharedThreadPool.PRIORITY.CACHED);
                while (connectionAcceptLooper.socketAccepted == null) {
                 //   Log.d("waiting for client", "go to sleep");
                    Thread.sleep(500);
                }
                Log.d("connecting:", "socket accepted");
            }
            return connectionAcceptLooper.socketAccepted;
        }

        private boolean procedure_transmission(OutputStream out_stream,FileNodeWrapper wrapper){
            monitor.receiveMessage(MessageCode.NOTICE_TRANSMITTING,null);
            monitor.onProgress("totalSize",wrapper.getTotalSize());
            Iterator<FileNode> iterator=wrapper.iterator();
            //BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(out_stream);
            final int blockSize=4*1024*1024;
            byte[] buffer=new byte[blockSize];
            long fileSize=0L;
            long bytesOfTransferred=0L;
            while(iterator.hasNext()){

                if(!monitor.abortSignal()) {
                    FileHandle handle = iterator.next().toFileHandle(wrapper.getRootPath());

                    //skip directory
                    if (handle.isFile()) {
                        try {
                            FileInputStream fileInputStream = new FileInputStream(handle.getFile());
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                            fileSize = handle.Size();

                            long blockCount = fileSize / blockSize;
                            int tailLength = (int) (fileSize % blockSize);
                            long blockOfTransferred = 0L;
                            while (blockOfTransferred < blockCount) {
                                bufferedInputStream.read(buffer);
                                out_stream.write(buffer);
                                //out_stream.flush();
                                //bufferedOutputStream.flush();
                                bytesOfTransferred += blockSize;
                                blockOfTransferred++;
                                Log.d("sending files", "procedure_transmission: byteOfTransferred: "+String.valueOf(bytesOfTransferred));
                                monitor.onSubProgress(0,handle.getAbsolutePathName(), bytesOfTransferred);
                            }
                            //transfer tail bytes
                            if (tailLength > 0) {
                                bufferedInputStream.read(buffer, 0, tailLength);
                                out_stream.write(buffer, 0, tailLength);
                                Log.d("sending files'tail", "procedure_transmission: byteOfTransferred");
                                out_stream.flush();
                                bytesOfTransferred += tailLength;
                                monitor.onSubProgress(0,handle.getAbsolutePathName(), bytesOfTransferred);
                            }

                            fileInputStream.close();
                            bufferedInputStream.close();
                            //Log.d("send files task", "procedure_transmission: finished");
                            //return true;

                        } catch (FileNotFoundException e) {
                            monitor.receiveMessage(MessageCode.ERR_FILE_NOT_FOUND, e.getMessage());
                            monitor.onStop(PROGRESS_STATUS.FAILED);
                            return false;
                        } catch (IOException e) {
                            monitor.receiveMessage(MessageCode.ERR_IO_EXCEPTION, e.getMessage());
                            monitor.onStop(PROGRESS_STATUS.FAILED);
                            return false;
                        }

                    }
                }
                else{ //task abort
                    monitor.onStop(PROGRESS_STATUS.ABORTED);
                    return  false;
                }
            }
            //loop end normally, return true here
            return true;

        }
    }

    /**
     * 监听客户端socket连接传入的类，在创建时需要指定剩余任务数。
     */
    private class ListenerAcceptLoop implements Runnable {

        Socket socketAccepted=null;
        ServerSocket serverSocket;
        ProgressMonitor<?,?> monitor;
        short clientCount;
        boolean terminateSignal=false;
        public ListenerAcceptLoop(ProgressMonitor<?,?> monitor, short clientCount){
            this.monitor=monitor;
            this.clientCount=clientCount;
            try {
                serverSocket=new ServerSocket(FMGlobal.ListenerPort);
                serverSocket.setSoTimeout(3*60*1000);
            } catch (IOException e) {
                monitor.receiveMessage(MessageCode.ERR_IO_EXCEPTION,e.getMessage());
                monitor.onStop(PROGRESS_STATUS.FAILED);
            }

        }

        @Override
        public void run() {
            while(!terminateSignal&&clientCount!=0){
                try {
                    Log.d("listener looper", "run: server is waiting()");
                    socketAccepted=serverSocket.accept();
                    clientCount--;
                }
                catch (SocketTimeoutException ex){ //超时后默认终止任务
                    monitor.receiveMessage(MessageCode.ERR_CONNECTION_TIMEOUT,ex.getMessage());
                    monitor.onStop(PROGRESS_STATUS.FAILED);
                    stopLoop();
                }
                catch (IOException e) {
                    monitor.receiveMessage(MessageCode.ERR_IO_EXCEPTION,e.getMessage());
                    monitor.onStop(PROGRESS_STATUS.FAILED);
                }
            }
        }

        public void stopLoop(){
            terminateSignal=true;
        }
    }


    public static class MessageCode{
        public static final int ERR_SOCKET_EXCEPTION=0;
        public static final int ERR_IO_EXCEPTION=1;
        public static final int ERR_INTERRUPT_EXCEPTION=2;
        public static final int ERR_CONNECTION_TIMEOUT=3;
        public static final int ERR_FILE_NOT_FOUND=4;
        public static final int ERR_UNKNOWN=8;

        public static final int NOTICE_CONNECT_DECLINED=5;
        public static final int NOTICE_CONNECTING=6;
        public static final int NOTICE_CONNECTED=7;
        public static final int NOTICE_TRANSMITTING=9;
    }

}
