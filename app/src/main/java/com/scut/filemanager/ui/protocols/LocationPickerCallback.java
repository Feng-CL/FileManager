package com.scut.filemanager.ui.protocols;

import android.os.Message;

import com.scut.filemanager.core.FileHandle;

public interface LocationPickerCallback {
    public void onLocationPicked(FileHandle location);
    public void onLocationPickerDialogCancel(FileHandle currentLocation, boolean whetherNeedToUpdateView);

}
