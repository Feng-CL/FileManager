package com.scut.filemanager.ui;


import android.content.Context;
import android.os.Build;
import android.os.TestLooperManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/*
@Description: 自定义一个项目装配器，用来装配相应数据到布局文件中。 ItemAssembler 仅仅是一个临时
名字，后期代码将为其重构为其他名字
 */
public class ItemAssembler extends BaseAdapter {

    private FileHandle folder=null;
    private ArrayList<FileHandle> list;
    private int item_layout_id;
    private Context context;


    public ItemAssembler(Context app_context, FileHandle Folder, int resource) throws Exception {
        folder=Folder;
        item_layout_id=resource;
        //should not happen
        if(folder==null)
            throw new NullPointerException("[ItemAssember:creator]: Folder cannot be empty");

        if(!folder.isDirectory()){
           throw new Exception("[ItemAssember:creator] argument isn't a folder");
        }

        int count=folder.getFileCount();
        if(count!=0){
            list=new ArrayList<FileHandle>(count);
            FileHandle[] file_array=folder.listFiles();
            for(int i=0;i<count;i++){
                list.add(file_array[i]);
            }
        }
        context=app_context;

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
    public View getView(int i, View view, ViewGroup parent) {
        String inflater=Context.LAYOUT_INFLATER_SERVICE; //获取服务名称
        //获取上下文的布局装配器
        LayoutInflater layoutInflater=(LayoutInflater)context.getSystemService(inflater);
        ConstraintLayout constraintLayout=null;
        if(getCount()==0){
            //无需装载，空目录
            constraintLayout=new ConstraintLayout(context);
            return constraintLayout;
        }
        else{
            FileHandle item=(FileHandle)getItem(i);
            constraintLayout= (ConstraintLayout) layoutInflater.inflate(item_layout_id,null);
            //装载图标
            ImageView imgView=constraintLayout.findViewById(R.id.imgview_item_icon);
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
            TextView item_textView=constraintLayout.findViewById(R.id.textview_item_name);
            TextView item_detail_textView=constraintLayout.findViewById(R.id.textview_item_detail);

                detailString.append(" size: "+com.scut.filemanager.util.textFormatter.byteCountDescriptionConvert_longToString(
                       "KB", item.Size(),1
                )+"KB "+
                        com.scut.filemanager.util.textFormatter.timeDescriptionConvert_simpleLongToString(
                                item.getLastModifiedTime()
                        )
                );

            item_textView.setText(item.getName());  item_detail_textView.setText(detailString.toString());
        }
        return constraintLayout;
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


    public void setFolder(FileHandle nextFolder){
        folder=nextFolder;
        updateItemSet();
        notifyDataSetChanged();
    }


    private void updateItemSet() {
        int item_count=folder.getFileCount();
        list.clear();
        if(item_count!=0){
            list.ensureCapacity(item_count);
            FileHandle[] file_array=folder.listFiles();
            for(int i=0;i<item_count;i++){
                list.add(file_array[i]);
            }
        }

    }
}
