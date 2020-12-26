package com.scut.filemanager.core.net;



import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class OnlineBoardCaster implements Runnable {



    DatagramSocket udpSocket; //socket used to send message
    private boolean stop=false; //status indicator
    private DatagramPacket alivePacket;
    private int statusCode=OnlineBoardCaster.STOP;
    private byte[] sndBuf=new byte[8*1024];

    static public final int NORMAL=0;
    static public final int STOP=1;
    static public final int SO_MULTICAST_DISABLED=2;
    static public final int MEET_IO_EXCEPTION=3;
    static public final int INTERUPPT_WHEN_SLEEP=4;


    static private OnlineBoardCaster caster=null;



    private OnlineBoardCaster(NetService netService){
        String deviceName=this.getClass().getName();
        try {
            InetAddress boardcastAddr=InetAddress.getByName("255.255.255.255");
            alivePacket = new DatagramPacket(deviceName.getBytes("utf-8"), deviceName.length(), boardcastAddr,33720 );
            udpSocket=new DatagramSocket();
            udpSocket.setBroadcast(true);
            if(!udpSocket.getBroadcast()){
                statusCode=OnlineBoardCaster.SO_MULTICAST_DISABLED;
            }
        }
        catch (UnknownHostException | SocketException | UnsupportedEncodingException ex){

        }
    }

    static public OnlineBoardCaster getInstance(NetService netService){
        if(caster==null){
            caster=new OnlineBoardCaster(netService);
        }
        return caster;
    }


    @Override
    public void run() {
        statusCode=OnlineBoardCaster.NORMAL;
        while(!stop){
            try {
                udpSocket.send(alivePacket);
                Thread.sleep(1000);
            }
            catch(IOException | InterruptedException ex){

            }
        }
        udpSocket.close();

    }

    public void stop(){
        stop=true;
    }
}

