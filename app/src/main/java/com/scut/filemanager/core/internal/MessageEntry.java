package com.scut.filemanager.core.internal;

import java.util.Map;

public class MessageEntry implements Map.Entry<Integer,String>{

    int code;
    String msg;

    public MessageEntry(int msgCode,String msg){
        code=msgCode;
        this.msg=msg;
    }

    @Override
    public Integer getKey() {
        return code;
    }

    @Override
    public String getValue() {
        return msg;
    }

    @Override
    public String setValue(String s) {
        return msg=s;
    }
}