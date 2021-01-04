package com.scut.filemanager.core.net;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class InquirePacket implements Serializable {

    public int what=MessageCode.IP_NULL;
    public Object obj=null;
    public String description="";
    public InetAddress ip=null;
    //public int length=0;

    public InquirePacket(int what){
        this.what=what;
    }


    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        ObjectOutputStream obj_output_stream=new ObjectOutputStream(byteArrayOutputStream);
        obj_output_stream.writeObject(this);
        obj_output_stream.flush();
        obj_output_stream.close();
        byte[] bytes= byteArrayOutputStream.toByteArray();
        return bytes;
    }

    static public InquirePacket decodeToThis(byte[] bytes) throws IOException{
        ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(bytes);
        ObjectInputStream obj_input_stream=new ObjectInputStream(byteArrayInputStream);
        try {
            InquirePacket inquirePacket = (InquirePacket) obj_input_stream.readObject();
            return inquirePacket;
        }
        catch (ClassNotFoundException ex) {
            Log.e("Inquire decode", "decodeToThis: class not found");
            return null;
        }

    }

    
    /*
        @Description: 描述类内部使用的信息码
    */
        
    static public class MessageCode{
        static public final int IP_NULL=0; //该包收到后只需当作节点发现包


        static public final int ACK_IP_FILES_AND_FOLDERS=3;
        static public final int N_ACK_IP_FILES_AND_FOLDER=2;
        static public final int IP_FILES_AND_FOLDERS=4;
    }



}
