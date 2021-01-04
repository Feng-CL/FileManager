package com.scut.filemanager.core.internal;

import androidx.annotation.NonNull;

import com.scut.filemanager.core.net.InquirePacket;

import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Hashtable;
import java.util.Queue;

abstract public class BoardCastScanWatcher extends AbstractTaskMonitor<InetAddress,String> {

    protected Hashtable<InetAddress, Queue<InquirePacket>> cacheTable=new Hashtable<>();
    
    @Override
    public void sendCancelSignal(int slot) {
        //null     
    }

    @Override
    protected void pushMessage(int code, String msg) {
        this.MessagesStack.push(new MessageEntry(code,msg));
    }

    /*
        @Description:这里将包压入缓冲表
        @Params:
    */

    public void pushInquirePacket(@NonNull InquirePacket packet){
        if(cacheTable.containsKey(packet.ip)){
            Queue<InquirePacket> packetQueue=cacheTable.get(packet.ip);
            packetQueue.add(packet);
        }
        else {
            cacheTable.put(packet.ip, new ArrayDeque<InquirePacket>());
        }
    }

    public InquirePacket dequeuePacketFromIp(InetAddress ip){
        Queue<InquirePacket> queue=cacheTable.get(ip);
        if(queue==null){
            return null;
        }
        else {
            return queue.poll();
        }
    }



    
}
