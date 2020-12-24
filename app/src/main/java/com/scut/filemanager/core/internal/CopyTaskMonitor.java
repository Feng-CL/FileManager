package com.scut.filemanager.core.internal;

import com.scut.filemanager.core.ProgressMonitor;
import com.scut.filemanager.util.SimpleArrayFilter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Future;

/*
复制任务监视器，
 */
public class CopyTaskMonitor extends AbstractTaskMonitor<String,Long> {

    HashMap<Integer,Long> tracker=new HashMap<>();
    protected long numberOfBytesNeedToCopy=0L;

    public CopyTaskMonitor(){
        //initial status
        cancelSignal=false;
        interruptSignal=false;
        MessagesStack=new Stack<>();
    }


    public long reportValueByTracker(){
        long sum=0L;
        Collection<Long> values=tracker.values();
        for (long l: values) {
            sum+=l;
        }
        return sum;
    }

    //特殊情况可能会使用到
    public HashMap<Integer,Long> getTracker(){
        return tracker;
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
        MessagesStack.push(new MessageEntry(code,msg));
    }


    @Override
    public void receiveMessage(int code,String msg){
        pushMessage(code, msg);
    }

    /*
    @Description: 从消息栈中弹出消息，typeFilter指定了弹出消息对应的消息码的范围
     */
    public String consumeMessage(Integer... typeFilter){
        Integer[] types=typeFilter;
        if(typeFilter.length==0){
            return MessagesStack.pop().getValue();
        }
        for (int i = 0; i < MessagesStack.size(); i++) {
            if(SimpleArrayFilter.hasElementByLinearSearch(types,
                    MessagesStack.get(i).getKey())){
                return MessagesStack.remove(i).getValue();
            }
        }
        return null;
    }

    public boolean hasMessage(){
        return !MessagesStack.isEmpty();
    }

    public MessageEntry popMessageEntry(){
        return  (MessageEntry) MessagesStack.pop();
    }


}
