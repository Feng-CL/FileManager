package com.scut.filemanager.ui.transaction;


import android.app.Activity;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.scut.filemanager.FileManager;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.ProgressMonitor;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.internal.AbstractTaskMonitor;
import com.scut.filemanager.core.internal.MessageEntry;
import com.scut.filemanager.core.net.FileNodeWrapper;
import com.scut.filemanager.core.net.InquirePacket;
import com.scut.filemanager.core.net.NetService;
import com.scut.filemanager.ui.dialog.LocationPickDialogDelegate;
import com.scut.filemanager.ui.dialog.ProgressDialogDelegate;
import com.scut.filemanager.ui.protocols.AbstractDialogCallBack;
import com.scut.filemanager.ui.protocols.LocationPickerCallback;
import com.scut.filemanager.ui.protocols.ProgressDialogContentProvider;
import com.scut.filemanager.util.FMFormatter;

import java.net.InetAddress;
import java.util.List;
import java.util.Stack;


/**
 * 该类需要借助NetService 类的能力来完成接收功能
 */
public class FileTransferTransactionMiddleWare extends AbstractDialogCallBack
        implements ProgressDialogContentProvider {

    //internal helper
    private ProgressDialogDelegate delegate=null;
    private TaskMonitorImpl monitor=new TaskMonitorImpl();

    //context references
    private NetService netService;
    private Activity context;

    //task type identifier
    private TaskType taskType;

    //other information
    private InetAddress address;

    //main thread operation
    Handler mHandler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case FileManager
                        .MAKE_TOAST:
                    String toast=(String)msg.obj;
                    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public FileTransferTransactionMiddleWare(TaskType type, Activity activity, NetService netService){
        this.netService=netService;
        this.context=activity;
        this.taskType=type;
    }

    /**
     * 为了将此类公用，分出了接受和发送的代理执行方法。
     */
    public void executeReceiveTask(InquirePacket packet, Service service){
        final InquirePacket packet_copy=packet;
        address=packet.ip;
        LocationPickDialogDelegate locationPicker=new LocationPickDialogDelegate(this.context, service, new LocationPickerCallback() {
            @Override
            public void onLocationPicked(FileHandle location) {
                netService.acceptAndSendACK(address);
                delegate=new ProgressDialogDelegate(context,FileTransferTransactionMiddleWare.this,ProgressDialogDelegate.ACTION.RECEIVE);
                delegate.showDialog();
                FileNodeWrapper wrapper_in_packet= (FileNodeWrapper) packet_copy.obj;
                wrapper_in_packet.setRootPath(location.getAbsolutePathName());
                //多余操作
                packet_copy.obj=wrapper_in_packet;
                netService.receive(packet_copy,monitor);
            }

            @Override
            public void onLocationPickerDialogCancel(FileHandle currentLocation, boolean whetherNeedToUpdateView) {
                //send a refuse packet back
                netService.refuseAndSendNACK(address);
            }

            @Override
            public void onDialogClose(DialogInterface dialog, boolean updateView) {

            }

            @Override
            public void onDialogCancel(DialogInterface dialog) {

            }

            @Override
            public void onDialogHide(DialogInterface dialog) {

            }

            @Override
            public void onDialogNeutralClicked(DialogInterface dialog) {

            }

            @Override
            public void onDialogOk(DialogInterface dialog) {

            }
        });
        locationPicker.showDialog();
    }

    public void executeSendTask(InetAddress target, List<FileHandle> listOfFiles){
        this.delegate=new ProgressDialogDelegate(this.context,this,ProgressDialogDelegate.ACTION.SEND);
        this.delegate.showDialog();
        this.address=target;
        this.netService.send(target,listOfFiles,monitor);
    }
    /**
     * 暂停键回调
     * @param dialog
     */
    @Override
    public void onDialogNeutralClicked(DialogInterface dialog) {
        monitor.sendInterruptSignal(!monitor.interruptSignal());
    }

    @Override
    public void onDialogHide(DialogInterface dialog) {
        dialog.dismiss();
    }

    @Override
    public void onDialogCancel(DialogInterface dialog) {
        monitor.sendCancelSignal();
    }

    @Override
    public int getMaxMeasure() {
        return 100;
    }

    @Override
    public boolean isPause() {
        return monitor.interruptSignal();
    }

    public ProgressMonitor<String,Long> getProgressMonitor(){
        return monitor;
    }

    public void toast(String toast){
        this.mHandler.sendMessage(
                Request.obtain(FileManager.MAKE_TOAST,toast)
        );
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
    protected class TaskMonitorImpl extends AbstractTaskMonitor<String,Long>{

        public TaskMonitorImpl(){
            cancelSignal=false;
            interruptSignal=false;
        }

        private long start=0L;
        private long numberOfBytesNeedToCopy=0L;
        @Override
        public void onStart() {
            super.onStart();
            MessagesStack=new Stack<>();
            StringBuilder task_desc=new StringBuilder();
            start= SystemClock.elapsedRealtime();
            if(taskType==TaskType.RECEIVE){
                task_desc.append("receiving files from "+address.getHostAddress());
            }
            else{
                task_desc.append("send files to ").append(address.getHostAddress()) ;
            }
            delegate.update_task_description(task_desc.toString());
            delegate.update_progress_bar(0,"0%");
            delegate.update_speed_description("calculating");

        }

        @Override
        public void onProgress(String key, Long value) {
            numberOfBytesNeedToCopy=value;
        }

        @Override
        public void onFinished() {
            delegate.update_progress_bar(100,"finished");

            String toast_content;
            if(taskType==TaskType.RECEIVE){
                toast_content="receive files successfully";
            }
            else{
                toast_content="send files successfully";
            }
            toast(toast_content);
            //close dialog finally
            delegate.closeDialog();

        }

        StringBuilder str_builder_task_desc=new StringBuilder();
        @Override
        public void onSubProgress(int taskId, String key, Long value) {
            str_builder_task_desc.setLength(0);
            long now=SystemClock.elapsedRealtime();
            if(taskType==TaskType.RECEIVE){
                str_builder_task_desc.append("receiving files from ").append(address.getHostAddress()).append(" to ")
                        .append(key);
            }
            else{
                str_builder_task_desc.append("sending file[ ").append(key).append(" ] to ").append(address.getHostAddress());
            }
            String speed_desc=calculateSpeed(now,start,value);
            String progress_desc="Finished: "+FMFormatter.d2s(calculateProgressf(value,numberOfBytesNeedToCopy),1)+"%";
            delegate.update_task_description(str_builder_task_desc.toString());
            delegate.update_progress_bar(calculateProgressi(value,numberOfBytesNeedToCopy),progress_desc);
            delegate.update_speed_description(speed_desc);
            delegate.update_time_ticking(FMFormatter.timeDescriptionConvert_ShortStyle_l2s(now-start));
        }

        @Override
        public void receiveMessage(int code, String msg) {
            String notice="";
            if(msg!=null) {
                notice=msg;
            }

            switch (code){
                case NetService.MessageCode.NOTICE_CONNECT_DECLINED:
                case NetService.MessageCode.ERR_FILE_NOT_FOUND:
                case NetService.MessageCode.ERR_INTERRUPT_EXCEPTION:
                case NetService.MessageCode.ERR_CONNECTION_TIMEOUT:
                case NetService.MessageCode.ERR_IO_EXCEPTION:
                case NetService.MessageCode.ERR_SOCKET_EXCEPTION:
                case NetService.MessageCode.ERR_UNKNOWN:
                    delegate.pop_notify_dialog(String.valueOf(code),notice);
                    //close delegate
                    delegate.closeDialog();
                    break;
                case NetService.MessageCode.NOTICE_CONNECTED:
                    delegate.update_task_description("connection established");
                    break;
                case NetService.MessageCode.NOTICE_CONNECTING:
                    delegate.update_task_description("connecting to target");
                    break;
                case NetService.MessageCode.NOTICE_TRANSMITTING:
                    delegate.update_title("transmitting");
                    break;
                default:
                    break;
            }

        }



        @Override
        public void sendCancelSignal(int slot) {

        }

        @Override
        protected void pushMessage(int code, String msg) {
            MessagesStack.push(new MessageEntry(code,msg));
        }
    }

    private String calculateSpeed(long now,long startTime, long byteOfCopied){
        long duration=now-startTime;  //ms
        String speed_desc;
        if(duration!=0) {
            long mean = byteOfCopied / duration; //  byte/ms
            mean*=1000; //byte/s
            speed_desc= FMFormatter.getSuitableFileSizeString(mean);
        }
        else{
            speed_desc="calculating";
        }
        return speed_desc+"/s";
    }


    private int calculateProgressi(long byteOfCopied,long numberOfBytesNeedToCopy){
        int progress= (int) (
                (((float)byteOfCopied)/numberOfBytesNeedToCopy)
                        *100
        );
        return progress;
    }

    private float calculateProgressf(long byteOfCopied,long numberOfBytesNeedToCopy){
        return ((float)byteOfCopied/numberOfBytesNeedToCopy)*100.0f;
    }

    public enum TaskType{
        SEND,RECEIVE
    }
}
