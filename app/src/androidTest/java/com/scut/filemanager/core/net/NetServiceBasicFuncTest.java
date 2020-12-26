package com.scut.filemanager.core.net;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.scut.filemanager.core.ProgressMonitor;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.util.FMFormatter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.SocketException;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
public class NetServiceBasicFuncTest {

    Context appContext;
    NetService netService;
    Service service;
    @Before
    public void setUp() throws Exception {
        appContext= InstrumentationRegistry.getInstrumentation().getTargetContext();
        service=Service.getInstance(appContext);
    }

    @Test
    public void test() throws SocketException {
        NetService netService=NetService.getInstance(service);
        netService.startBoardCaster();
        netService.startScanner();
        while(true){

        }
    }
    @After
    public void tearDown() throws Exception {
    }
}