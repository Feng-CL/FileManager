package com.scut.filemanager.ui.adapter;


import android.os.Build;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;


import android.widget.TextView;

import androidx.annotation.RequiresApi;


import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.FileHandleFilter;
import com.scut.filemanager.util.SimpleArrayFilter;
import com.scut.filemanager.util.Sorter;
import com.scut.filemanager.util.TextFormatter;
import com.scut.filemanager.util.protocols.DisplayFolderChangeResponder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/*
@Description: 自定义一个项目装配器，用来装配相应数据到布局文件中。 ItemAssembler 仅仅是一个临时
名字，后期代码将为其重构为其他名字
 */
public class SimpleListItemAssembler extends BaseAdapter {

    private FileHandle folder=null;
    private ArrayList<FileHandle> list;
    private int checkBoxVisibility;
    private java.util.ArrayList<Boolean> selectedTable;
    private int item_layout_id;
    private LayoutInflater inflater;
    private List<DisplayFolderChangeResponder> responders=new ArrayList<>();

    private Comparator<FileHandle> comparator=null;

    private static Comparator<FileHandle> default_comparator=new Comparator<FileHandle>() {
        @Override
        public int compare(FileHandle f1, FileHandle f2) {
            String str_f1=f1.getName(); String str_f2=f2.getName();
            return str_f1.compareTo(str_f2);
        }
    };



    public SimpleListItemAssembler(FileHandle Folder, int resource,LayoutInflater inflater) throws Exception {
        folder=Folder;
        item_layout_id=resource;
        this.inflater=inflater;
        //should not happen
        if(folder==null)
            throw new NullPointerException("[ItemAssember:creator]: Folder cannot be empty");

        if(!folder.isDirectory()){
           throw new Exception("[ItemAssember:creator] argument isn't a folder");
        }

        checkBoxVisibility=View.INVISIBLE;
        int count=folder.getFileCount();
        if(count!=0){
            list=new ArrayList<FileHandle>(count);
            FileHandle[] file_array=folder.listFiles();
            for(int i=0;i<count;i++){
                list.add(file_array[i]);
            }
        }

        selectedTable=new ArrayList<>();
        this.refreshSelectedTable();

    }

    public void registerFolderChangedResponder(DisplayFolderChangeResponder responder){
        if(!responders.contains(responder)){
            responders.add(responder);
        }
    }

    /*
    后期为了降低RequiresApiLevel ， 可能需要另外使用其它包，或自己写一个排序算法了
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sortAscending(){
        list.sort(
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
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        int count;
        if(list==null)
            count=0;
        else
            count=list.size();
        return count;
    }

    @Override
    public Object getItem(int i) {
        if(list==null) {
            //may happen 
            return null;
        }
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override

    /*
    目前加载view的方法容易阻塞，即是一次刷新的架构，需要等待所有的空间内容都加载出来后
    其父控件才开始显示其内容，为此为了打开文件比较多的文件夹时，需要考虑使用双线程显示与内容计算分离的方法。
     */
    //listView attempts to reuse view objects in order to improve performance and avoid a lag in response to user scrolls.
    public View getView(int i, View convertView, ViewGroup parent) {

        if(getCount()==0){
            //无需装载，空目录
            return null;
        }
        else{
            FileHandle item=(FileHandle)getItem(i);
            if(convertView==null) {
                convertView = inflater.inflate(item_layout_id, parent, false);
            }

            //装载图标
            ImageView imgView=convertView.findViewById(R.id.imgview_item_icon);
            int  itemCount_underfolder=0; StringBuilder detailString=new StringBuilder();
            if(item.isDirectory()){
                imgView.setImageResource(R.drawable.icon_default_dir);

                    itemCount_underfolder=item.getFileCount();

                detailString.append("Total: "+itemCount_underfolder);
            }
            else{
                imgView.setImageResource(R.drawable.icon_raw_file);
            }
            //装载文字
            TextView item_textView=convertView.findViewById(R.id.textview_item_name);
            TextView item_detail_textView=convertView.findViewById(R.id.textview_item_detail);

                detailString.append(" size: "+ TextFormatter.byteCountDescriptionConvert_longToString(
                       "KB", item.Size(),1
                )+"KB "+
                        TextFormatter.timeDescriptionConvert_simpleLongToString(
                                item.getLastModifiedTime()
                        )
                );

            item_textView.setText(item.getName());  item_detail_textView.setText(detailString.toString());

            //调整checkBox 状态
            CheckBox checkBox=convertView.findViewById(R.id.item_checkbox);
            checkBox.setVisibility(checkBoxVisibility);
            checkBox.setChecked(selectedTable.get(i));
        }
        return convertView;
    }

    public int getResourceId(String name)
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

    public void setSelectedAtItem(int i,boolean check){
        selectedTable.set(i,check);
    }

    public void setFolder(FileHandle nextFolder){
        folder=nextFolder;
        updateItemSet();
        checkBoxVisibility=View.INVISIBLE;
        refreshSelectedTable();
        notifyDataSetChanged();
        dispatchFolderChangedEventToResponders();
    }

    public static void  setDefaultComparator(Comparator<FileHandle> comparator){
        default_comparator=comparator;
    }

    public void setComparator(Comparator<FileHandle> cmptor){
        comparator=cmptor;
    }


    public void refreshSelectedTable(){
        int count=getCount();
        int currentSize=selectedTable.size();
        if(count<=currentSize){
            if(count*3<currentSize){  //shrink the table
                for (int i = currentSize-1; i>count-1 ; i--) {
                    selectedTable.remove(i);
                }
            }
            for (int i = 0; i < count; i++) {
                selectedTable.set(i,false);
            }
        }
        else{
            for (int i = 0; i < currentSize; i++) {
                selectedTable.set(i,false);
                Log.d("SimpleListItemAssembler","code run");
            }
            for (int i = currentSize; i < count; i++) {
                selectedTable.add(false);
            }
        }
    }

    public void refreshSelectedTableToAllSelected(){
        for (int i = 0; i < selectedTable.size(); i++) {
            selectedTable.set(i,true);
        }
    }

    public void refreshSelectedTableToAllUnSelected(){
        for (int i = 0; i <selectedTable.size(); i++) {
            selectedTable.set(i,false);
        }
    }

    public void updateCheckBoxDisplayState(int Visibility){
        checkBoxVisibility=Visibility;
        notifyDataSetChanged();
    }

    public void setListAssembled(FileHandle[] listToAssemble){
        list.clear();
        list.addAll(Arrays.asList(SimpleArrayFilter.filter(listToAssemble, new FileHandleFilter() {
            @Override
            public boolean accept(FileHandle handle) {
                return handle!=null;
            }
        })));
        notifyDataSetChanged();
        dispatchFolderChangedEventToResponders();

    }

    private void updateItemSet() {
        int item_count=folder.getFileCount();
        list.clear();
        if(item_count!=0){
            list.ensureCapacity(item_count);
            FileHandle[] file_array=folder.listFiles();
            //sort them,set up comparator
            Comparator<FileHandle> useComparator;
            useComparator=comparator!=null?comparator:default_comparator;

            Sorter.mergeSort(file_array,useComparator,4);
            Log.d("ItemAssembler","sorts items successfully");

            for(int i=0;i<item_count;i++){
                list.add(file_array[i]);
            }
        }
    }

    private void dispatchFolderChangedEventToResponders(){
        for (DisplayFolderChangeResponder responder:responders
             ) {
            responder.respondTo(this.folder);
        }
    }





}
