package com.scut.filemanager.ui.adapter;

import androidx.annotation.WorkerThread;

import com.scut.filemanager.R;

import java.util.EnumMap;
import java.util.HashMap;

public class IconResourceIdHelper  {
    static EnumMap<FileType,Integer> fileTypeMapper;
    static HashMap<String,FileType> stringToFileTypeMapper;

    @WorkerThread
    static void preLoad(){
        fileTypeMapper=_initFileTypeToResIdMapper();
        stringToFileTypeMapper=_initStringToFileTypeMapper();
    }

    static private EnumMap<FileType,Integer> _initFileTypeToResIdMapper(){
        EnumMap<FileType,Integer> mapper= new EnumMap<>(FileType.class);
        mapper.put(FileType.APK,R.drawable.ic_icon_basic_apk);
        mapper.put(FileType.AUDIO,R.drawable.ic_icon_basic_audio);
        mapper.put(FileType.EXCEL,R.drawable.ic_icon_basic_excel);
        mapper.put(FileType.HTML,R.drawable.ic_icon_basic_html);
        mapper.put(FileType.JPG,R.drawable.ic_icon_basic_jpg);
        mapper.put(FileType.MARKDOWN,R.drawable.ic_icon_basic_md);
        mapper.put(FileType.MKV,R.drawable.ic_icon_basic_video);
        mapper.put(FileType.MP4,R.drawable.ic_icon_basic_mp4);
        mapper.put(FileType.MP3,R.drawable.ic_icon_basic_mp3);
        mapper.put(FileType.PDF,R.drawable.ic_icon_basic_pdf);
        mapper.put(FileType.PNG,R.drawable.ic_icon_basic_png);
        mapper.put(FileType.VIDEO,R.drawable.ic_icon_basic_video);
        mapper.put(FileType.RMVB,R.drawable.ic_icon_basic_video);
        mapper.put(FileType.PPT,R.drawable.ic_icon_basic_ppt);
        mapper.put(FileType.WORD,R.drawable.ic_icon_basic_word);
        mapper.put(FileType.UNKNOWN,R.drawable.ic_icon_basic_unknown);
        mapper.put(FileType.RAR,R.drawable.ic_icon_basic_rar);
        mapper.put(FileType.ZIP,R.drawable.ic_icon_basic_zip);
        mapper.put(FileType.TXT,R.drawable.icon_raw_file);

        return mapper;
    }

    /**
     * called after first call preLoad()
     * @param name
     * @return
     */
    static public int getIconResourceIdByFileName(String name) {
        int last_index_of_dot = name.lastIndexOf('.');
        String extension_name = null;

        //no extension name
        if (last_index_of_dot == -1 || last_index_of_dot == name.length() - 1) {
            extension_name = "";
        } else {
            extension_name = name.substring(last_index_of_dot + 1);
        }

        FileType correspondFileType=stringToFileTypeMapper.get(extension_name);
        if(correspondFileType==null){
            return fileTypeMapper.get(FileType.UNKNOWN);
        }
        return fileTypeMapper.get(correspondFileType);
    }


    static private HashMap<String,FileType> _initStringToFileTypeMapper(){
        HashMap<String,FileType> mapper=new HashMap<>(30,0.9f);

        //package
        mapper.put("apk",FileType.APK);
        //audio
        mapper.put("audio",FileType.AUDIO);
        mapper.put("mp3",FileType.MP3);
        mapper.put("flac",FileType.AUDIO);
        mapper.put("ogg",FileType.AUDIO);
        mapper.put("wav",FileType.AUDIO);
        mapper.put("m4a",FileType.AUDIO);

        //image
        mapper.put("png", FileType.PNG);
        mapper.put("jpg",FileType.JPG);
        mapper.put("jpeg",FileType.JPG);

        //video
        mapper.put("video",FileType.VIDEO);
        mapper.put("mp4",FileType.MP4);
        mapper.put("wma",FileType.VIDEO);
        mapper.put("rm",FileType.RMVB);
        mapper.put("rmvb",FileType.RMVB);
        mapper.put("flv",FileType.VIDEO);
        mapper.put("mov",FileType.VIDEO);
        mapper.put("mkv",FileType.VIDEO);
        mapper.put("mpg",FileType.VIDEO);
        mapper.put("mpeg",FileType.VIDEO);
        mapper.put("avi",FileType.VIDEO);

        //office
        mapper.put("word",FileType.WORD);
        mapper.put("doc",FileType.WORD);
        mapper.put("docx",FileType.WORD);
        mapper.put("ppt",FileType.PPT);
        mapper.put("pptx",FileType.PPT);
        mapper.put("excel",FileType.EXCEL);
        mapper.put("xls",FileType.EXCEL);
        mapper.put("xlsx",FileType.EXCEL);
        mapper.put("xlsb",FileType.EXCEL);
        mapper.put("xlsm",FileType.EXCEL);
        mapper.put("xlst",FileType.EXCEL);
        mapper.put("pdf",FileType.PDF);

        //html
        mapper.put("html",FileType.HTML);
        mapper.put("htm",FileType.HTML);
        mapper.put("xml",FileType.XML);
        mapper.put("md",FileType.MARKDOWN);
        mapper.put("markdown",FileType.MARKDOWN);

        //compress tar
        mapper.put("tar",FileType.TAR);
        mapper.put("zip",FileType.ZIP);
        mapper.put("rar",FileType.RAR);

        //plain text
        mapper.put("txt",FileType.TXT);

        //unknown type
        mapper.put("",FileType.UNKNOWN);

        return mapper;
    }

}
