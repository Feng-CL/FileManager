package com.scut.filemanager.ui.transaction;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.SystemClock;

import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.internal.CopyTaskMonitor;
import com.scut.filemanager.core.internal.MessageEntry;
import com.scut.filemanager.ui.controller.BaseController;
import com.scut.filemanager.ui.controller.TabViewController;
import com.scut.filemanager.ui.dialog.ProgressDialogDelegate;
import com.scut.filemanager.ui.protocols.ProgressDialogContentProvider;
import com.scut.filemanager.util.FMFormatter;

import java.util.List;


public class CopyTransactionProxy extends CopyTaskMonitor
        implements ProgressDialogContentProvider {

    //context and handler
    private BaseController parentController;
    private Handler targetHandler;
    private long startTime,now;
    private String dstPath;
    private FileHandle[] selectedFiles;
    private ProgressDialogDelegate progressDialogDelegate;
    //private int currentSubTaskId=0;
    //status


    //此类完成校验任务

    public CopyTransactionProxy(List<FileHandle> listOfFiles,String dstPath,BaseController parentController){
        super();
        FileHandle[] fileArray=new FileHandle[listOfFiles.size()];
        for (int i = 0; i < fileArray.length; i++) {
            fileArray[i]=listOfFiles.get(i);
        }
        this.parentController=parentController;
        this.dstPath=dstPath;
        this.selectedFiles=fileArray;

    }

    public void execute(){

        if(selectedFiles.length==1){
            //invoke dialog
            progressDialogDelegate=new ProgressDialogDelegate(parentController.getFileManagerCoreService().getContext(),
                    this,ProgressDialogDelegate.ACTION.COPY);
            this.targetHandler=progressDialogDelegate.getHandler();
            progressDialogDelegate.showDialog();

            parentController.getFileManagerCoreService().copy(selectedFiles[0],dstPath,this,false,
                    Service.Service_CopyOption.RECURSIVE_COPY,Service.Service_CopyOption.REPLACE_EXISTING);
        }
        else if(selectedFiles.length>1){
            //invoke dialog
            this.progressDialogDelegate=new ProgressDialogDelegate(parentController.getFileManagerCoreService().getContext(),
                    this,ProgressDialogDelegate.ACTION.COPY);
            this.targetHandler=this.progressDialogDelegate.getHandler();
            this.progressDialogDelegate.showDialog();
            parentController.getFileManagerCoreService().copy(selectedFiles,dstPath,this,false,
                    Service.Service_CopyOption.REPLACE_EXISTING,Service.Service_CopyOption.RECURSIVE_COPY);
        }

    }

    @Override
    public int getMaxMeasure() {
        return 100;
    }

    @Override
    public boolean isPause() {
        return interruptSignal;
    }


    @Override
    public void onDialogClose(DialogInterface dialog,boolean updateView) {
        if(updateView){
            if(parentController instanceof TabViewController){
                TabViewController c= (TabViewController) parentController;
                c.setDisplayFolder(c.getCurrentLocationFileHandle());
            }
        }
    }

    @Override
    public void onDialogCancel(DialogInterface dialog) {
        /*
        主线程的进度对话框dismiss之后，this.targetHandler将可能会被垃圾回收，这里需要注意
        因为在后续线程结束的时候还会调用onFinished,而onFinished此时不能使用targetHandler来更新UI，改用Toast
         */
        sendCancelSignal();
    }

    private boolean isDialogHide=false;
    @Override
    public void onDialogHide(DialogInterface dialog) {
        isDialogHide=true;
    }

    @Override
    public void onDialogNeutralClicked(DialogInterface dialog) { //onPaused
        if(isPause()){
            interruptSignal=false;
            startTime=System.currentTimeMillis(); //reset startTime
            tracker.clear(); //reset tracker , this operation will reset the variable "byteOfCopied"

        }
        else {
            //notify UI now is pausing
            this.targetHandler.sendMessage(
                    Request.obtain(ProgressDialogDelegate.UIMessageCode.UPDATE_SPEED_DESC,"paused")
            );
            interruptSignal = true;
        }
    }

    @Override
    public void onDialogOk(DialogInterface dialog) {

    }

    @Override
    public void onStart() { //这里是主线程的时间
        //notify
        super.onStart();
        startTime= SystemClock.elapsedRealtime();

        //通知正在估算进度
        this.targetHandler.sendMessage(
                Request.obtain(ProgressDialogDelegate.UIMessageCode.UPDATE_PROGRESS_BAR,0,"calculating")
        );
        //通知速度
        this.targetHandler.sendMessage(Request.obtain(
                ProgressDialogDelegate.UIMessageCode.UPDATE_SPEED_DESC,"calculating"
        ));
    }

    @Override
    public void onFinished() {
        if(targetHandler!=null) {
            if (progress_status == PROGRESS_STATUS.GOING) {
                if (cancelSignal) {
                    progress_status = PROGRESS_STATUS.ABORTED;
                    this.parentController.makeToast("copy task cancelled");
                } else {
                    progress_status = PROGRESS_STATUS.COMPLETED;
                    //refresh UI
                    this.targetHandler.sendMessage(
                            Request.obtain(ProgressDialogDelegate.UIMessageCode.UPDATE_PROGRESS_BAR, 100, "Finished")
                    );
                    this.parentController.makeToast("copy successfully");
                }
            } else {
                //通知UI显示错误
                while (hasMessage()) {
                    String[] msgArray = {"null", "null"};
                    MessageEntry messageEntry = popMessageEntry();
                    msgArray[0] = messageEntry.getKey().toString();
                    msgArray[1] = messageEntry.getValue();
                    this.targetHandler.sendMessage(
                            Request.obtain(ProgressDialogDelegate.UIMessageCode.POP_NOTIFY_DIALOG, msgArray)
                    );
                }
                this.parentController.makeToast("something wrong");

            }

            //无论如何都要关闭dialog
            this.targetHandler.sendEmptyMessage(ProgressDialogDelegate.UIMessageCode.CLOSE_DIALOG);
        }
        else{
            //这里只有两种种情况，那就是点击了hide和cancel
            if(isDialogHide){
                this.parentController.makeToast("hided dialog");
            }
            else{ //cancel 的情况
                this.parentController.makeToast("task aborted");
            }
        }

    }

    @Override
    public void onSubTaskStop(int taskId, PROGRESS_STATUS status) {
        this.progress_status=status;
    }

    @Override
    public void describeTask(int taskId, String title) {
        String task_description="Now copying: ".concat(title);
        this.targetHandler.sendMessage(
                Request.obtain(ProgressDialogDelegate.UIMessageCode.UPDATE_TASK_DESC,task_description)
        );
    }

    @Override
    public void onSubTaskStart(int taskId) {
        super.onSubTaskStart(taskId);
    }


    @Override //通告mHandler
    public void onSubProgress(int taskId, String key, Long value) {
        if(!isDialogHide) {
            this.getTracker().put(taskId, value);
            now = SystemClock.elapsedRealtime();
            long byteOfCopied = reportValueByTracker();
            int progress_finished = calculateProgress(byteOfCopied);
            this.targetHandler.sendMessage(
                    Request.obtain(ProgressDialogDelegate.UIMessageCode.UPDATE_PROGRESS_BAR,
                            progress_finished, "finished: " + progress_finished + "%"
                    )
            );
            this.targetHandler.sendMessage(
                    Request.obtain(ProgressDialogDelegate.UIMessageCode.UPDATE_SPEED_DESC,
                            calculateSpeed(now, byteOfCopied))
            );
            long duration=now-startTime;
            String time_duration_hhMMss=FMFormatter.timeDescriptionConvert_ShortStyle_l2s(duration);
            this.progressDialogDelegate.update_time_ticking(time_duration_hhMMss);
        }
    }


    @Override
    public void onProgress(String key, Long value) {
        numberOfBytesNeedToCopy=value;
    }

    private String calculateSpeed(long now, long byteOfCopied){
        long duration=now-startTime;  //ms
        String speed_desc;
        if(duration!=0) {
            long mean = byteOfCopied / duration; //  byte/ms
            mean*=1000; //byte/s
            speed_desc=FMFormatter.getSuitableFileSizeString(mean);
        }
        else{
            speed_desc="calculating";
        }
        return speed_desc+"/s";
    }


    private int calculateProgress(long byteOfCopied){
        int progress= (int) (
                (((float)byteOfCopied)/numberOfBytesNeedToCopy)
        *100
        );
        return progress;
    }



}
