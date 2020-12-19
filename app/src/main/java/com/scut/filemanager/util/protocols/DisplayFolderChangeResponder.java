package com.scut.filemanager.util.protocols;

import com.scut.filemanager.core.FileHandle;

//响应显示文件夹变化的接口
public interface DisplayFolderChangeResponder {
    public void respondTo(FileHandle folder);
}
