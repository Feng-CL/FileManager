package com.scut.filemanager.ui.adapter;


import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.scut.filemanager.FileManager;
import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.FileHandleFilter;
import com.scut.filemanager.core.concurrent.SharedThreadPool;
import com.scut.filemanager.ui.controller.TabViewController;
import com.scut.filemanager.util.SimpleArrayFilter;
import com.scut.filemanager.util.Sorter;
import com.scut.filemanager.util.FMFormatter;
import com.scut.filemanager.util.protocols.DisplayFolderChangeResponder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;


/*
@Description: 自定义一个项目装配器，用来装配相应数据到布局文件中。 ItemAssembler 仅仅是一个临时
名字，后期代码将为其重构为其他名字
 */
public class SimpleListViewItemAssembler extends BaseAdapter {

    //数据源
    private FileHandle folder=null;
    //组织
    private ArrayList<FileHandle> list_of_files=new ArrayList<>();
    private List<ItemData> itemDataList=new ArrayList<>();//UI更新时从这里取内容
    //layout
    private int item_layout_id;
    private LayoutInflater inflater;
    private int checkBoxVisibility=View.INVISIBLE;
    //callbacks
    private List<DisplayFolderChangeResponder> responders=new ArrayList<>();

    //sorter
    private Comparator<FileHandle> comparator=null;
    private static Comparator<FileHandle> default_comparator= FileManager.Default_FileHandleComparator;

    //status maintain


    //task
    private AssembleTask assembleTask;

    //relative controllers
    private TabViewController parentTabViewController;

    static private class MessageCode{
        static private final int NOTIFY_DATASET_CHANGE=0;
        static private final int NOTIFY_LOADING=1;
        static private final int NOTIFY_LOADED=2;
    }

    //handler 处理ui更新
    private Handler mHandler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MessageCode.NOTIFY_LOADING:
                    parentTabViewController.updateProgressBarVisibility(View.VISIBLE);
                    break;
                case MessageCode.NOTIFY_LOADED:
                    notifyDataSetChanged();
                    parentTabViewController.updateProgressBarVisibility(View.INVISIBLE);
                case MessageCode.NOTIFY_DATASET_CHANGE:
                    notifyDataSetChanged();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    public SimpleListViewItemAssembler(FileHandle Folder, TabViewController bindController) throws Exception {
        folder=Folder;
        item_layout_id=R.layout.list_item;
        parentTabViewController=bindController;
        this.inflater=parentTabViewController.getLayoutInflater();
        //should not happen
        if(folder==null)
            throw new NullPointerException("[ItemAssember:creator]: Folder cannot be empty");

        if(!folder.isDirectory()){
           throw new Exception("[ItemAssember:creator] argument isn't a folder");
        }
        notifyDataSetChanged();
        assembleTask=new AssembleTask();
        setFolder(folder);

    }

    /*
    @Description：注册文件夹改变的回调类，不直接使用，通过父控制器使用，
     */
    public void registerFolderChangedResponder(DisplayFolderChangeResponder responder){
        if(!responders.contains(responder)){
            responders.add(responder);
        }
    }

    /*
        @Description:
    */

    public void setCheckBoxVisibility(int visibility){
        checkBoxVisibility=visibility;
        mHandler.sendEmptyMessage(MessageCode.NOTIFY_DATASET_CHANGE);
    }

    public void setAllItemDataCheckedState(boolean state){
        //hasCheckedItem=state;
        for (ItemData item:itemDataList
             ) {
            item.isChecked=state;
        }
    }

    public List<FileHandle> getSelectedFileHandles(){
        ArrayList<FileHandle> selected_handles_list=new ArrayList<>(list_of_files.size());
        for (int i = 0; i < itemDataList.size(); i++) {
            if(itemDataList.get(i).isChecked){
                selected_handles_list.add(list_of_files.get(i));
            }
        }
        return selected_handles_list;
    }

    /*
        @Description:获取选中文件，如果选中多个文件，此时为第一个文件，否则为空
    */

    public FileHandle getSelectedFile(){
        int index=-1;
        for (int i = 0; i < itemDataList.size(); i++) {
            if(itemDataList.get(i).isChecked){
                index=i;
            }
        }
        if(index!=-1){
            return list_of_files.get(index);
        }
        else {
            return null;
        }
    }

    /*
    后期为了降低RequiresApiLevel ， 可能需要另外使用其它包，或自己写一个排序算法了
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sortAscending(){
        list_of_files.sort(
                new Comparator<FileHandle>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public int compare(FileHandle f1, FileHandle f2) {
                        boolean f1_isDir=f1.isDirectory();
                        boolean f2_isDir=f2.isDirectory();

                        if(f1_isDir^f2_isDir){
                            //who is a directory is preceding
                            if(f1_isDir){
                                return -1; //f1<f20
                            }
                            else{
                                return 1;
                            }
                        }

                        //both are file or directory
                        return f1.getName().compareTo(f2.getName());
                    }
                }
        );
        //notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        int count;
        if(itemDataList==null)
            count=0;
        else
            count= itemDataList.size();
        return count;
    }

    @Override
    public Object getItem(int i) {
        if(itemDataList==null||i>itemDataList.size()-1||i<0){
            return null;
        }
        return itemDataList.get(i);
    }

    public FileHandle getFileHandleAtPosition(int i){
        if(list_of_files==null||i>list_of_files.size()-1||i<0){
            return null;
        }
        return list_of_files.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }




    //listView attempts to reuse view objects in order to improve performance and avoid a lag in response to user scrolls.

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View getView(int i, View convertView, ViewGroup parent) {

        if(getCount()==0){
            //无需装载，空目录
            return null;
        }
        else{
            //FileHandle item=(FileHandle)getItem(i);
            ItemData itemData=(ItemData)getItem(i);
            if(itemData!=null) {
                synchronized (itemData) {
                    if (convertView == null) { //reuse view
                        convertView = inflater.inflate(item_layout_id, parent, false);
                    }

                    ImageView imgView = convertView.findViewById(R.id.imgview_item_icon);
                    TextView textView = convertView.findViewById(R.id.textview_item_name);
                    TextView textView_detail = convertView.findViewById(R.id.textview_item_detail);

                    imgView.setImageResource(itemData.resId);//strange
                    textView.setText(itemData.itemName);
                    textView_detail.setText(itemData.itemDetailInfo);


                    //装载图标
//            ImageView imgView=convertView.findViewById(R.id.imgview_item_icon);
//            int  itemCount_underfolder=0; StringBuilder detailString=new StringBuilder();
//            if(item.isDirectory()){
//                imgView.setImageResource(R.drawable.icon_default_dir);
//
//                    itemCount_underfolder=item.getFileCount();
//
//                detailString.append("Total: "+itemCount_underfolder);
//            }
//            else{
//                int iconResId=getIconResourceIdByFileName(item.getName());
//                imgView.setImageResource(iconResId);
//            }
//            //装载文字
//            TextView item_textView=convertView.findViewById(R.id.textview_item_name);
//            TextView item_detail_textView=convertView.findViewById(R.id.textview_item_detail);
//
//                detailString.append(" size: "+ TextFormatter.byteCountDescriptionConvert_longToString(
//                       "KB", item.Size(),1
//                )+"KB "+
//                        TextFormatter.timeDescriptionConvert_simpleLongToString(
//                                item.getLastModifiedTime()
//                        )
//                );
//
//            item_textView.setText(item.getName());  item_detail_textView.setText(detailString.toString());
//
//            //调整checkBox 状态
                    CheckBox checkBox = convertView.findViewById(R.id.item_checkbox);
                    //根据保存的状态调整checkbox可见性
                    checkBox.setVisibility(checkBoxVisibility);
                    //根据数据集调整checkbox的选中状态
                    checkBox.setChecked(itemData.isChecked);
                    checkBox.setClickable(false);
                }
            }
            //checkBox.setChecked(selectedTable.get(i));
        }
        return convertView;
    }

    public void setSelectAll(){
        //hasCheckedItem=true;
        for (ItemData item :
                itemDataList) {
            item.isChecked = true;
        }
        mHandler.sendEmptyMessage(MessageCode.NOTIFY_LOADED);
    }


    @Deprecated
    private int getResourceId(String name)
    {
        try
        {
            //getField 获取变量名字段
            java.lang.reflect.Field field = R.drawable.class.getField(name);
            //转换成整数表示
            //使用Field.get获取变量值时，参数设为null即可，如果不是静态变量，需要为Field.get方法指
            // 定一个变量所在的类的对象作为参数
            return Integer.parseInt(field.get(null).toString());
        }
        catch (Exception e)
        {

        }
        return 0;
    }

    public FileHandle getCurrentFolder(){
        return folder;
    }



    public void setFolder(@NonNull FileHandle nextFolder){
        folder=nextFolder;
        SharedThreadPool.getInstance().executeTask(assembleTask, SharedThreadPool.PRIORITY.HIGH);
        dispatchFolderChangedEventToResponders();


        //notifyDataSetChanged();

    }

    public static void  setDefaultComparator(Comparator<FileHandle> comparator){
        default_comparator=comparator;
    }

    public void setComparator(Comparator<FileHandle> cmptor){
        comparator=cmptor;
    }

    //重置数据集
    public void setListAssembled(FileHandle[] listToAssemble){
        mHandler.sendEmptyMessage(MessageCode.NOTIFY_LOADING);
        list_of_files.clear();
        list_of_files.addAll(Arrays.asList(SimpleArrayFilter.filter(listToAssemble, new FileHandleFilter() {
            @Override
            public boolean accept(FileHandle handle) {
                return handle!=null;
            }
        })));

        itemDataList.clear();
        for (int i = 0; i < listToAssemble.length; i++) {
            if(listToAssemble[i]!=null) {
                ItemData itemData = new ItemData();
                itemData.itemName = listToAssemble[i].getName();
                itemData.resId = R.drawable.icon_default_dir;
                itemData.itemDetailInfo = "";
                itemDataList.add(itemData);
            }
        }


        mHandler.sendEmptyMessage(MessageCode.NOTIFY_LOADED);
        //notifyDataSetChanged();

        dispatchFolderChangedEventToResponders();

    }

    /*
    @Description: 清空设置adapter 内所有item 的ischecked
     */

    private void updateItemSet() {
        int item_count=folder.getFileCount();
        list_of_files.clear();
        if(item_count!=0){
            list_of_files.ensureCapacity(item_count);
            FileHandle[] file_array=folder.listFiles();
            //sort them,set up comparator
            Comparator<FileHandle> useComparator;
            useComparator=comparator!=null?comparator:default_comparator;

            Sorter.mergeSort(file_array,useComparator,4);
           // Log.d("ItemAssembler","sorts items successfully");

            for(int i=0;i<item_count;i++){
                list_of_files.add(file_array[i]);
            }
        }
    }


    private void dispatchFolderChangedEventToResponders(){
        for (DisplayFolderChangeResponder responder:responders
             ) {
            responder.respondTo(this.folder);
        }
    }

    private int getIconResourceIdByFileName(String name) {
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


    static EnumMap<FileType,Integer> fileTypeMapper= SimpleListViewItemAssembler._initFileTypeToResIdMapper();
    static HashMap<String,FileType> stringToFileTypeMapper= SimpleListViewItemAssembler._initStringToFileTypeMapper();

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

    static private EnumMap<FileType,Integer> _initFileTypeToResIdMapper(){
        EnumMap<FileType,Integer> mapper=new EnumMap<FileType, Integer>(FileType.class);
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

    //异步线程处理好应该加载的数据，并通过Message提交到主线程中,可复用的实例
 
    private List<ItemData> tempItemList=new ArrayList<>(30);

    class AssembleTask implements Runnable{

        public AssembleTask(){

        }

        @Override
        public void run() {
            SimpleListViewItemAssembler.this.mHandler.sendEmptyMessage(MessageCode.NOTIFY_LOADING);
                updateItemSet();

            tempItemList.clear();

            for (int i = 0; i < list_of_files.size(); i++) {

                FileHandle handle = list_of_files.get(i);
                ItemData itemData = new ItemData();
                itemData.itemName = handle.getName();

                StringBuilder detailInfo = new StringBuilder();

                if (handle.isDirectory()) {
                    itemData.resId = R.drawable.icon_default_dir;
                    detailInfo.append("Total: " + handle.getFileCount());
                } else {
                    itemData.resId = getIconResourceIdByFileName(handle.getName());
                }
                detailInfo.append("  size: " + FMFormatter.getSuitableFileSizeString(handle.Size()));
                detailInfo.append(" " + FMFormatter.timeDescriptionConvert_LongStyle_l2s(handle.getLastModifiedTime()));
                itemData.itemDetailInfo = detailInfo.toString();


                tempItemList.add(itemData);

            }
            synchronized (itemDataList) {
                itemDataList.clear();
                itemDataList.addAll(tempItemList);
            }
            SimpleListViewItemAssembler.this.mHandler.sendEmptyMessage(MessageCode.NOTIFY_LOADED);
        }
    }



    public class ItemData{
        String itemName="";
        String itemDetailInfo="  ";
        public boolean isChecked=false;
        int resId=R.drawable.icon_raw_file;
    }



}
