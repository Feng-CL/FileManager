package com.scut.filemanager.core.net;

import android.util.Log;

import androidx.annotation.NonNull;

import com.scut.filemanager.FMGlobal;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.ProgressMonitor;
import com.scut.filemanager.core.concurrent.SharedThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;

/**
接收方，创建socket向服务端发送连接请求，元信息从NetService类中，取得。
 */


public class FileReceiverClient {

    InetAddress src_address;
    FileNodeWrapper wrapper;
    ProgressMonitor<String,Long> monitor;

    public FileReceiverClient(InetAddress src_address, FileNodeWrapper wrapper,ProgressMonitor<String,Long> monitor){
        this.src_address=src_address;
        this.wrapper=wrapper;
        this.monitor=monitor;
    }

    //调用该方法前应该向对方发送ACK
    public void startClient(){
        SharedThreadPool.getInstance().executeTask(new FileReceiveTask(),SharedThreadPool.PRIORITY.CACHED);
    }

    private boolean procedure_constructDirectoryStructure(FileNodeWrapper wrapper){
        boolean result=true;
        Iterator<FileNode> iterator=wrapper.iterator();
        iterator.next();
        while(iterator.hasNext()){
           FileNode node=iterator.next();
           if(node.isDirectory()){
               Log.d("mkdir: ", wrapper.getRootPath().concat(node.getPath()));
               result=result&&node.toFileHandle(wrapper.getRootPath()).makeDirectory();
           }
        }
        return result;
    }


    /**
     * ProgressMonitor 的协议： onProgress汇报进度总估计，onSubProgress汇报当前进度
     * @param inputStream
     * @param wrapper
     * @param monitor
     * @throws IOException
     * @throws InterruptedException
     */
    private void procedure_constructFileFromInputStream(InputStream inputStream, FileNodeWrapper wrapper, ProgressMonitor<String,Long> monitor)
            throws IOException, InterruptedException {
        monitor.onProgress("totalSize",wrapper.getTotalSize());
        byte[] buffer=new byte[FMGlobal.Default_BlockSize];
        long bytesOfTransferred=0L;
        BufferedInputStream bufferedInputStream=new BufferedInputStream(inputStream,4*FMGlobal.Default_BlockSize);
        Iterator<FileNode> iterator=wrapper.iterator();

        while(iterator.hasNext()){
            if(!monitor.abortSignal()) {
                FileNode node = iterator.next();
                if(node.isFile()) {
                    long fileSize = node.size;
                    FileHandle handle = node.toFileHandle(wrapper.getRootPath());
                    int blockCount = (int) (fileSize / FMGlobal.Default_BlockSize);  //文件块数量
                    int blockOfTransferred = 0;
                    int tailLength = (int) (fileSize % FMGlobal.Default_BlockSize);  //文件尾部长度
                    FileOutputStream fileOutputStream = new FileOutputStream(handle.getFile());
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    while (blockOfTransferred < blockCount) {
                        //wait until enough data arrived
//                        int available=bufferedInputStream.available();
//                        while (available< FMGlobal.Default_BlockSize) {
//                            Thread.sleep(200);
//                            Log.d("receive files","available(): "+available);
//                            if (monitor.abortSignal()) {
//                                fileOutputStream.close();
//                                bufferedOutputStream.close();
//                                bufferedInputStream.close();
//                                return;
//                            }
//                        }
                        int byte_in_Block_transferred=0;
                        while(byte_in_Block_transferred<FMGlobal.Default_BlockSize) {
                            int read_result = bufferedInputStream.read(buffer, byte_in_Block_transferred, FMGlobal.Default_BlockSize-byte_in_Block_transferred);
                            byte_in_Block_transferred+=read_result;
                        }

                        bufferedOutputStream.write(buffer);
                        blockOfTransferred++;
                        bytesOfTransferred += FMGlobal.Default_BlockSize;
//                        Log.d("receiving files block", "procedure_constructFileFromInputStream: byteOfTransferred: "+String.valueOf(bytesOfTransferred).concat(
//                                " result of read: "+String.valueOf(read_result)+" available(): "+String.valueOf(bufferedInputStream.available())
//                        ));
                        monitor.onSubProgress(0, handle.getName(), bytesOfTransferred);
                    }
                    if (tailLength > 0) {
//                        while (bufferedInputStream.available() < tailLength) {
//                            Thread.sleep(200);
//                            if (monitor.abortSignal()) {
//                                fileOutputStream.close();
//                                bufferedOutputStream.close();
//                                bufferedInputStream.close();
//                                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.ABORTED);
//                                return;
//                            }
//                        }
                        int offset=0;
                        while(offset<tailLength) {
                            int read_result = bufferedInputStream.read(buffer, offset, tailLength-offset);
                            offset+=read_result;
                        }
                        bufferedOutputStream.write(buffer, 0, tailLength);
                        bytesOfTransferred += tailLength;
                        //Log.d("receiving files' tail ", "procedure_constructFileFromInputStream: byteOfTransferred: "+String.valueOf(bytesOfTransferred).concat(" result of read: "+String.valueOf(read_result)));
                        monitor.onSubProgress(0, handle.getName(), bytesOfTransferred);
                    }
                    //next file
                }
            }
            else{
                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.ABORTED);
            }
        }
    }

    private class FileReceiveTask implements Runnable{

        @Override
        public void run() {
            monitor.onStart();
            Socket socket=new Socket();
            try {
                monitor.receiveMessage(NetService.MessageCode.NOTICE_CONNECTING,null);
                Thread.sleep(1500);
                Log.d(this.getClass().getName() , "run: connect to"+src_address.getHostAddress());
                socket.connect(new InetSocketAddress(src_address,FMGlobal.ListenerPort),60*1000); //1 min connection timeout
                monitor.receiveMessage(NetService.MessageCode.NOTICE_CONNECTED,null);
                if(procedure_constructDirectoryStructure(wrapper)){
                    monitor.receiveMessage(NetService.MessageCode.NOTICE_TRANSMITTING,null);
                    procedure_constructFileFromInputStream(socket.getInputStream(),wrapper,monitor);
                    monitor.onFinished();
                }
                else{
                    monitor.receiveMessage(NetService.MessageCode.ERR_IO_EXCEPTION,"create directories failed");
                    monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
                }
            } catch (IOException ex) {
                monitor.receiveMessage(NetService.MessageCode.ERR_IO_EXCEPTION,ex.getMessage());
                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            } catch (InterruptedException ex){
                monitor.receiveMessage(NetService.MessageCode.ERR_INTERRUPT_EXCEPTION,ex.getMessage());
                monitor.onStop(ProgressMonitor.PROGRESS_STATUS.FAILED);
            }
        }
    }

}