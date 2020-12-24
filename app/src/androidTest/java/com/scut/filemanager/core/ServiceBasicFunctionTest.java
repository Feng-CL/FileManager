package com.scut.filemanager.core;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

@RunWith(AndroidJUnit4.class)
public class ServiceBasicFunctionTest implements ProgressMonitor<String,Float>{

    String src_path="/storage/emulated/0/Movies/Download";
    String dst_path="/storage/emulated/0/Documents";
    String src_dst_path=dst_path+"/Download";
    Service service;
    boolean working_signal=true;

    @Before
    public void setUp() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        service=Service.getInstance(appContext);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void copy() throws InterruptedException {
//        FileHandle src=new FileHandle(src_path);
//        assertTrue("service starts normally ",service.getStatus()== Service.SERVICE_STATUS.OK);
//        long startTime=System.currentTimeMillis();
//        dst_path=service.getPathUnderSdCard("Movies");
//        service.copy(src,dst_path, this,false,Service.Service_CopyOption.RECURSIVE_COPY);
//
//        //wait until copyTask is done
//        while(working_signal){
//
//        }
//
//        long costTime=System.currentTimeMillis()-startTime;
//        System.out.println("cost time : "+costTime+"ms");
//        assertTrue(service.ExistsAtPath(src_dst_path));
    }

    @Test
    public void moveFunc()  {

        src_path=service.getPathUnderRootDir("Movies")+"/Download";
        dst_path=service.getPathUnderSdCard("Movies");
        FileHandle src=new FileHandle(src_path);
        service.move(src,dst_path,Service.Service_CopyOption.REPLACE_EXISTING,this);
        while(working_signal){

        }
    }

    @Override
    public void onProgress(String key, Float value) {
        System.out.println(key+":"+value);
    }

    @Override
    public void onFinished() {
        System.out.println("all task finish");
        working_signal=false;
    }

    @Override
    public void onStart() {
        System.out.println("task start");
        working_signal=true;
    }

    @Override
    public void onStart(Future<?> future) {

    }

    @Override
    public void onStop(PROGRESS_STATUS status) {
        System.out.println("task stop"+" , status "+status.name());
        working_signal=false;
    }

    @Override
    public void onSubProgress(int taskId, String key, Float value) {

    }

    @Override
    public void onSubTaskStart(int taskId) {
        System.out.println("SubTask"+taskId+" start");
    }

    @Override
    public void onSubTaskStop(int taskId, PROGRESS_STATUS status) {

    }

    @Override
    public void onSubTaskFinish(int taskId) {
        System.out.println("SubTask "+taskId+" finish");
    }

    @Override
    public void receiveMessage(String msg) {
        System.out.println("receiveMessage: "+msg);
    }

    @Override
    public void receiveMessage(int code, String msg) {
        System.out.println("msg code: "+code+" msg: "+msg);
    }

    @Override
    public void describeTask(int taskId, String title) {

    }

    @Override
    public boolean interruptSignal() {
        return false;
    }

    @Override
    public boolean abortSignal() {
        return false;
    }

    @Override
    public void setUpAbortSignalSlot(boolean[] slot) {

    }
}