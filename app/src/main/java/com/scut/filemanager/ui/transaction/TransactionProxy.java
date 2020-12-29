package com.scut.filemanager.ui.transaction;

import android.os.Message;

import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.ui.BaseController;

/*
    @Description: 一种高耦合，低内聚的较差的自定义设计模式，事务代理
                主要用于区分更新UI和事务操作的区别
                代理视图控制器做一些后台操作，并通知前台更新视图，具体代理方式由
                对应控制器（Director)决定

                一般在处理请求的类中，定义ProxyMessageCode，否则会引起不同类定义的ProxyMessageCode对应的id值相同
                接收请求的类根据自己的代理能力过滤出来自其他类发送的ProxyMessageCode 并执行对应任务
    @Notice: 代理模式应该使用统一的信息码。
        早期项目实践中的错误：
                由于在其他类中看来，只有一个BaseController引用，不做instanceof操作判断时。
                无法知道应该使用哪些信息码，这在一定程度上，提高了发起请求的类对接收请求的类
                的信息精确要求。这对于降低软件设计的耦合度，是及其不利的。不过也好在该模式还没有彻底广泛使用，修改
                模式的代价较低。

 */
public class TransactionProxy {

    private BaseController director;



    public TransactionProxy(BaseController ProxyController){
        director=ProxyController;
    }


    public void sendRequest(Message message){

    }

}
