package com.scut.filemanager.core.net;

import android.util.Log;

import androidx.annotation.NonNull;

import com.scut.filemanager.core.ProgressMonitor;
import com.scut.filemanager.core.internal.BoardCastScanWatcher;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Timer;

public class BoardCastScanner implements Runnable {

    static InetSocketAddress MultiCastAddress = new InetSocketAddress(33720);
    private DatagramSocket udpSocket;
    private boolean stopScanning = false;
    private BoardCastScanWatcher watcher;
    private byte[] buf = new byte[8 * 1024];
    private DatagramPacket rcvPacket = null;

    public BoardCastScanner(@NonNull DatagramSocket bindUdpSocket,@NonNull BoardCastScanWatcher watcher) {
        this.udpSocket = bindUdpSocket;
        try {
            this.udpSocket.setSoTimeout(15 * 1000);
//            this.udpSocket.setReceiveBufferSize(); //8KB SO_RCVBUF
        } catch (SocketException socketException) {
            watcher.receiveMessage(WatcherMsgCode.RESET_TIMEOUT_FAILED,socketException.getMessage());
            watcher.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
        }
        this.watcher = watcher;
    }


    /*
    @Protocols:
    监视器的键值类型：设备名称：ip地址

    watcher 信息码：

     */
    static public class WatcherMsgCode{
        static final int PERIOD_TIMEOUT=0;
        static final int SOCKET_EXCEPTION_HAPPENS=1;
        static final int RESET_TIMEOUT_FAILED=2;
        static final int INTERRUPT_EXCEPTION=3;
    }

    @Override
    public void run() {
        if(rcvPacket==null){
            rcvPacket=new DatagramPacket(buf,buf.length);
        }

        watcher.onStart();
        while (!stopScanning) {
            try {
                udpSocket.receive(rcvPacket);
                byte[] subBytes=Arrays.copyOf(rcvPacket.getData(),rcvPacket.getLength());
                InquirePacket inquirePacketReceived=InquirePacket.decodeToThis(subBytes);
                inquirePacketReceived.ip=rcvPacket.getAddress();
                //Log.d("boardcastScanner",String.valueOf(inquirePacketReceived.what));
                //空心跳包由onProgress处理，控制信号包压入缓冲表
                watcher.onProgress(inquirePacketReceived.ip,inquirePacketReceived);

                if(watcher.abortSignal()){
                    stopScanning=true;
                }
            }
            catch(SocketTimeoutException timeoutException){
                watcher.receiveMessage(WatcherMsgCode.PERIOD_TIMEOUT,timeoutException.getMessage());
                //watcher.onStop(ProgressMonitor.PROGRESS_STATUS.PAUSED); //扫描器暂时超时
                while(watcher.interruptSignal()){
                    try {
                        this.wait(1000);
                    } catch (InterruptedException e) {
                        watcher.receiveMessage(WatcherMsgCode.INTERRUPT_EXCEPTION,e.getMessage());
                        watcher.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED); //此时应该停止扫描
                        stop();
                    }
                }
            }
            catch (IOException ioex){
                watcher.receiveMessage(WatcherMsgCode.SOCKET_EXCEPTION_HAPPENS,ioex.getMessage());
                watcher.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                stop();
                //return;
            }
        }

        Log.i("scanner: ","stop scanning");
        watcher.onFinished();
    }


    public boolean isScanning(){
        return !stopScanning;
    }
    public void stop(){
        stopScanning=true;
    }
}
