package com.scut.filemanager.core.net;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Message;

import androidx.annotation.NonNull;

import com.scut.filemanager.FMGlobal;
import com.scut.filemanager.DeviceSelectActivity;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.ProgressMonitor;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.internal.BoardCastScanWatcher;
import com.scut.filemanager.ui.adapter.DeviceListViewAdapter;
import com.scut.filemanager.ui.transaction.Request;
import com.scut.filemanager.util.FMFormatter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

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


    public void send(InetAddress targetAddress, List<FileHandle> listOfFiles,ProgressMonitor<String,Long> monitor){
        String rootPrefix="/";
        if(listOfFiles.size()>0){

        }
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
    public void onProgress(InetAddress key, String value) {

        DeviceListViewAdapter.ItemData itemData=new DeviceListViewAdapter.ItemData(key.hashCode());
        itemData.DeviceIp=key.getHostAddress();
        itemData.DeviceName=value;
        Message message=Message.obtain();
        message.what= DeviceSelectActivity.UIMessageCode.NOTIFY_DATASET_CHANGE;
        message.obj=itemData;
        this.deviceSelectActivity.mHandler.sendMessage(message);
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
    private class SendFilesTask implements Runnable{

        ProgressMonitor<String,Long> monitor;
        List<FileHandle> listOfFiles;
        InetAddress targetAddress;

        public SendFilesTask(InetAddress address, List<FileHandle> fileHandles, ProgressMonitor<String,Long> monitor){
            this.monitor=monitor;
            this.listOfFiles=fileHandles;
            targetAddress=address;
        }

        @Override
        public void run(){
            monitor.onStart();
            try {
                DatagramSocket udpSocket=new DatagramSocket();
                udpSocket.setSoTimeout(60*1000);

                InquirePacket inquirePacket=prepareInquirePacket(listOfFiles);
                try {
                    byte[] bytesOfInquirePacket=inquirePacket.getBytes();
                    DatagramPacket packet=new DatagramPacket(bytesOfInquirePacket,bytesOfInquirePacket.length,targetAddress,33720);

                    udpSocket.send(packet);
                    //waiting
                    short respond=waitForACK(targetAddress);
                    while(respond==0){
                        //sleeping
                        try{
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            monitor.receiveMessage(MessageCode.ERR_INTERRUPT_EXCEPTION,e.getMessage() );
                            monitor.onStop(PROGRESS_STATUS.FAILED);
                            return;
                        }
                        respond=waitForACK(targetAddress);
                    }
                    //check whether opposite deny the connect request
                    if(respond==-1){
                        monitor.receiveMessage(MessageCode.NOTICE_CONNECT_DECLINED,"connect request is declined");
                        monitor.onStop(PROGRESS_STATUS.ABORTED);
                        return;
                    }

                    //prepare to build up connection


                    
                } catch (IOException e) {
                    monitor.receiveMessage(MessageCode.ERR_IO_EXCEPTION,e.getMessage());
                    monitor.onStop(PROGRESS_STATUS.FAILED);
                    return;
                }
            } catch (SocketException e) {
                monitor.receiveMessage(MessageCode.ERR_SOCKET_EXCEPTION,e.getMessage() );
                monitor.onStop(PROGRESS_STATUS.FAILED);
                return;
            }
            monitor.onFinished();
        }


        private InquirePacket prepareInquirePacket( List<FileHandle> list){
            //temporary create a virtual parent folder to wrap the files in list
            InquirePacket inquirePacket=new InquirePacket(InquirePacket.MessageCode.IP_FILES_AND_FOLDERS);
            FileNode root=FileNode.createNodeFromList("wrap",0,list);
            inquirePacket.obj=root;
            return inquirePacket;
        }

        private short waitForACK(InetAddress ip){
            InquirePacket inquirePacketFromIp=NetService.this.cacheTable.get(ip).poll();
            if(inquirePacketFromIp==null){
                return 0;
            }
            else {
                if (inquirePacketFromIp.what == InquirePacket.MessageCode.ACK_IP_FILES_AND_FOLDERS) {
                    return 1;
                } else if (inquirePacketFromIp.what == InquirePacket.MessageCode.N_ACK_IP_FILES_AND_FOLDER) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }


    public static class MessageCode{
        public static final int ERR_SOCKET_EXCEPTION=0;
        public static final int ERR_IO_EXCEPTION=1;
        public static final int ERR_INTERRUPT_EXCEPTION=2;
        public static final int NOTICE_CONNECT_DECLINED=3;
    }

}
