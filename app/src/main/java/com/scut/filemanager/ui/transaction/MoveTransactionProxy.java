package com.scut.filemanager.ui.transaction;

import android.content.DialogInterface;
import android.os.Handler;

import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.core.internal.AbstractTaskMonitor;
import com.scut.filemanager.core.internal.MessageEntry;
import com.scut.filemanager.ui.BaseController;
import com.scut.filemanager.ui.TabViewController;
import com.scut.filemanager.ui.dialog.ProgressDialogDelegate;
import com.scut.filemanager.ui.protocols.ProgressDialogContentProvider;

import java.util.List;
import java.util.Stack;


public class MoveTransactionProxy extends AbstractTaskMonitor<String,Float> implements ProgressDialogContentProvider {

    private BaseController parentController;
    private Handler targetHandler;
    private String dstPath;
    private List<FileHandle> selectedFiles;

    public MoveTransactionProxy(List<FileHandle> selections, String dstPath, BaseController parentController){
        super();
        this.MessagesStack=new Stack<>();
        this.parentController=parentController;
        this.dstPath=dstPath;
        this.selectedFiles=selections;
    }

    public void execute(){
        if(selectedFiles.size()>0){
            ProgressDialogDelegate delegate=new ProgressDialogDelegate(parentController.getFileManagerCoreService().getContext(),
                    this,ProgressDialogDelegate.ACTION.MOVE);
            this.targetHandler=delegate.getHandler();
            delegate.showDialog();

            parentController.getFileManagerCoreService().move(
                    selectedFiles,dstPath, Service.Service_CopyOption.REPLACE_EXISTING,this
            );
        }
    }

    @Override
    public int getMaxMeasure() {
        return selectedFiles.size();
    }

    @Override
    public void onStart() {
        progress_status=PROGRESS_STATUS.GOING;
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
    public void onStop(PROGRESS_STATUS status) {
        progress_status=status;
        onFinished();
    }

    @Override
    public void onFinished() {
        if(targetHandler!=null){

            if(progress_status==PROGRESS_STATUS.GOING){
                if (cancelSignal) {
                    progress_status = PROGRESS_STATUS.ABORTED;

                } else {
                    progress_status = PROGRESS_STATUS.COMPLETED;
                    //refresh UI
                    this.targetHandler.sendMessage(
                            Request.obtain(ProgressDialogDelegate.UIMessageCode.UPDATE_PROGRESS_BAR, 100, "Finished")
                    );
                    this.parentController.makeToast("move successfully");
                }
            }
            else{
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
                this.parentController.makeToast("progress: ");
            }


            //无论如何都要关闭dialog
            this.targetHandler.sendEmptyMessage(ProgressDialogDelegate.UIMessageCode.CLOSE_DIALOG);
        }
        else {
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
    public boolean isPause() {
        return interruptSignal;
    }

    public boolean hasMessage(){
        return !this.MessagesStack.isEmpty();
    }

    @Override
    public void onDialogClose(DialogInterface dialogInterface, boolean updateView) {
        if(updateView){
            if(parentController instanceof TabViewController){
                TabViewController c= (TabViewController) parentController;
                c.setDisplayFolder(c.getCurrentLocationFileHandle());
            }
        }
    }

    @Override
    public void onDialogCancel(DialogInterface dialogInterface) {
        sendCancelSignal(0);
    }

    public MessageEntry popMessageEntry(){
        return (MessageEntry) MessagesStack.pop();
    }

    private boolean isDialogHide=false;
    @Override
    public void onDialogHide(DialogInterface dialogInterface) {
        isDialogHide=true;
    }

    @Override
    public void onDialogNeutralClicked(DialogInterface dialogInterface) {
        if(isPause()){
            interruptSignal=false;
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
    public void onDialogOk(DialogInterface dialogInterface) {
        //onDialogHide 代替
    }

    @Override
    public void sendCancelSignal(int slot) {
        switch (slot){
            case 0:
                cancelSignal=true;
                break;
            default:
                if(abortSignalSlot!=null&&slot<=abortSignalSlot.length){
                    abortSignalSlot[slot-1]=true;
                }
                break;
        }
    }

    @Override
    protected void pushMessage(int code, String msg) {
        this.MessagesStack.push(new MessageEntry(code,msg));
    }

    @Override
    public void onProgress(String key, Float value) {
        int progress_value= (int) (value*100);
        this.targetHandler.sendMessage(
                Request.obtain(
                        ProgressDialogDelegate.UIMessageCode.UPDATE_TASK_DESC,
                        "Moving "+key
                )
        );

        this.targetHandler.sendMessage(
                Request.obtain(
                        ProgressDialogDelegate.UIMessageCode.UPDATE_PROGRESS_BAR,
                        progress_value,
                        "finished: "+progress_value+"%"
                )
        );
    }

    @Override
    public void receiveMessage(int code,String msg){
        pushMessage(code, msg);
    }
}
