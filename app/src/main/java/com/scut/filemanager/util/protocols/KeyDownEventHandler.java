package com.scut.filemanager.util.protocols;

import android.view.KeyEvent;
import android.widget.AdapterView;

import java.io.IOException;

public interface KeyDownEventHandler {
    public boolean onKeyDownEventHandleFunction(AdapterView<?> parentView, int keyCode, KeyEvent keyEvent) throws IOException;
}
