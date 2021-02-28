package com.scut.filemanager.util.protocols;

import com.scut.filemanager.core.net.NetService;

public interface WifiStateChangeListener {
    void onWifiStateChange(NetService.NetStatus wifi_status); //executed after isIdle()
    boolean isIdle(); //如在一次检测中，判定为Idle则会移除该钩子
}
