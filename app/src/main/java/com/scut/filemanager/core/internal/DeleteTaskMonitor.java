package com.scut.filemanager.core.internal;

import java.util.Stack;

public class DeleteTaskMonitor extends AbstractTaskMonitor<String,Boolean>{

    public DeleteTaskMonitor(){
        cancelSignal=false;
        interruptSignal=false;
        MessagesStack=new Stack<>();
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

    public boolean hasMessage(){
        return !MessagesStack.isEmpty();
    }

    public MessageEntry popMessageEntry(){
        return  (MessageEntry) MessagesStack.pop();
    }

    @Override
    public void receiveMessage(int code,String msg){
        pushMessage(code, msg);
    }
}
