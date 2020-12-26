package com.scut.filemanager.core.net;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.internal.AbstractTaskMonitor;
import com.scut.filemanager.core.internal.MessageEntry;
import com.scut.filemanager.util.FMFormatter;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

//该类是启动和控制服务的起点，
public class NetService extends AbstractTaskMonitor<String,InetAddress> {

    //网络适配器
    private WifiManager wifi;
    private WifiInfo wifiInfo=null;
    private Enumeration<NetworkInterface> netInterfaces;

    //dependent filemanager.core.service
    private Service service;

    //save status
    private NetStatus status=NetStatus.UNINITIALIZED;

    //static members
    private static NetService netService=null;
    //

    //function members
    private OnlineBoardCaster caster;
    private BoardCastScanner scanner;

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
            status=NetStatus.WIFI_PERMISSION_DENY;
        }
        else {

            int wifiStatusi=wifi.getWifiState();
            switch(wifiStatusi){
                case WifiManager.WIFI_STATE_DISABLED:
                case WifiManager.WIFI_STATE_DISABLING:
                    status=NetStatus.WIFI_DISABLED;
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                case WifiManager.WIFI_STATE_ENABLED:
                    status=NetStatus.WIFI_ENABLED;
                    break;
                default:
                    status=NetStatus.WIFI_STATUS_UNKNOWN;
            }

            if(status==NetStatus.WIFI_ENABLED){
                wifiInfo=wifi.getConnectionInfo();
                if(wifiInfo.getIpAddress()!=-1){
                    //wifi enabled but not connect or getIpAddress
                    status=NetStatus.WIFI_CONNECTED;
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

    public void startScanner() throws SocketException {
        DatagramSocket udpSocket=new DatagramSocket(33720); //33720 listen
        this.scanner=new BoardCastScanner(udpSocket,this);
        threads[0]=new Thread(this.scanner);
        threads[0].start();
    }

    public void startBoardCaster() throws SocketException {
        this.caster=OnlineBoardCaster.getInstance(this);
        threads[1]=new Thread(this.caster);
        threads[1].start();
    }

    public NetStatus getStatus(){
        return status;
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

    /*
    @Description: return null if netServiceStatus isn't WIFI_CONNECTED
     */
    public String getWifiIpAddressDesc(){
        if(getStatus()==NetStatus.WIFI_CONNECTED){
            return FMFormatter.ip4Address_i2s(wifiInfo.getIpAddress());
        }
        return null;
    }

    @Override
    public void sendCancelSignal(int slot) {
        //empty
    }

    @Override
    protected void pushMessage(int code, String msg) {
        this.MessagesStack.push(new MessageEntry(code,msg));
    }

    @Override
    public void receiveMessage(int code, String msg) {
        Log.d("code: "+code ,"msg: "+msg);
    }

    @Override
    public void onProgress(String key, InetAddress value) {
        Log.i("receive: ", key+" from: "+value.getHostName());
        //this.cancelSignal=true;
        //this.caster.stop();
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



}