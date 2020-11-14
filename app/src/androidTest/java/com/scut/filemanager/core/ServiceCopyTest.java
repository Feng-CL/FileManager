package com.scut.filemanager.core;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ServiceCopyTest implements ProgressMonitor<String,Long>{

    String src_path="/storage/emulated/0/Download";
    String dst_path="/storage/emulated/0/Movies";
    String src_dst_path=dst_path+"/Download";
    Service service;
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
        FileHandle src=new FileHandle(src_path);
        assertTrue("service starts normally ",service.getStatus()== Service.SERVICE_STATUS.OK);
        long startTime=System.currentTimeMillis();
        Thread copyThread=service.copy(src,dst_path, this,Service.Service_CopyOption.RECURSIVE_COPY);
        copyThread.join();
        long costTime=System.currentTimeMillis()-startTime;
        System.out.println("cost time : "+costTime+"ms");
        assertTrue(service.ExistsAtPath(src_dst_path));
    }

    @Override
    public void onProgress(String key, Long value) {
        System.out.println(key+":"+value);
    }

    @Override
    public void onFinished() {
        System.out.println("all task finish");
    }

    @Override
    public void onStart() {
        System.out.println("task start");
    }

    @Override
    public void onStop(PROGRESS_STATUS status) {
        System.out.println("task stop"+" , status "+status.name());
    }

    @Override
    public void onSubProgress(int taskId, String key, Long value) {
        System.out.println("taskid"+taskId+" "+key+":"+value);
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
    public boolean interruptSignal() {
        return false;
    }

    @Override
    public boolean abortSignal() {
        return false;
    }
}