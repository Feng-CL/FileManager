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



    private OnlineBoardCaster(NetService netService)  {
        try {
            InetAddress boardcastAddr=InetAddress.getByName("255.255.255.255");
            alivePacket = new DatagramPacket(sndBuf,0,boardcastAddr,33720 );
            this.constructDefaultEmptyPacket();
            udpSocket=new DatagramSocket();
            udpSocket.setBroadcast(true);
            if(!udpSocket.getBroadcast()){
                statusCode=OnlineBoardCaster.SO_MULTICAST_DISABLED;
            }
            statusCode=OnlineBoardCaster.NORMAL;
        }
        catch (UnknownHostException | SocketException ex){
            statusCode=OnlineBoardCaster.STOP;
        }
        catch(IOException ioex){
            statusCode=MEET_IO_EXCEPTION;
        }
    }

    static public OnlineBoardCaster getInstance(NetService netService){
        if(caster==null){
            caster=new OnlineBoardCaster(netService);
        }
        return caster;
    }

    public boolean checkStatus(int expect){
        return expect==this.statusCode;
    }



    synchronized public DatagramPacket constructDefaultEmptyPacket() throws IOException {
        InquirePacket inquirePacket=new InquirePacket(InquirePacket.MessageCode.IP_NULL);
        inquirePacket.description=NetService.getDeviceModel();
        alivePacket.setData(inquirePacket.getBytes());
        return alivePacket;
    }

    @Override
    public void run() {

        if(statusCode!=OnlineBoardCaster.NORMAL){
            return;
        }

        while(!stop){
            try {
                synchronized (alivePacket) {
                    udpSocket.send(alivePacket);
                }
                Thread.sleep(2000);
            }
            catch(IOException ioex){
                this.statusCode=OnlineBoardCaster.MEET_IO_EXCEPTION;
            }
            catch (InterruptedException ex){
                this.statusCode=OnlineBoardCaster.INTERUPPT_WHEN_SLEEP;
            }
        }
        udpSocket.close();
        statusCode=OnlineBoardCaster.STOP;
    }

    public void stop(){
        stop=true;
    }
}

