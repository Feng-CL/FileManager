package com.scut.filemanager.ui.adapter;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.RecyclerView;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.FileHandleFilter;
import com.scut.filemanager.util.FMFormatter;
import com.scut.filemanager.util.protocols.DisplayFolderChangeResponder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryViewAdapter extends AbsRecyclerLinearAdapter {

    public static final int LINEAR=1;
    public static final int GRID=2;

    int viewType=LINEAR;
    int checkBoxVisibility=View.INVISIBLE;

    private AssembleTask assembleTask=null;
    private FileHandle folder=null;
    FileHandleFilter[] filters=null;


    //callbacks
    private List<DisplayFolderChangeResponder> responders=new ArrayList<>();

    public DirectoryViewAdapter(FileHandle folder,int viewType){
        super(R.layout.item_view_directory_linear);
        if(viewType==GRID){
            viewHolderLayoutId=R.layout.item_view_directory_grid;
        }
    }

    public DirectoryViewAdapter(List<AbsRecyclerLinearAdapter.ItemData> data){
        super(data, R.layout.item_view_directory_linear);
    }

    public DirectoryViewAdapter(List<AbsRecyclerLinearAdapter.ItemData> data,int viewType) {
        super(data,R.layout.item_view_directory_linear);
        if(viewType==GRID){
            viewHolderLayoutId=R.layout.item_view_directory_grid;
        }
    }

    public void setCheckBoxVisibility(int checkBoxVisibility) {
        this.checkBoxVisibility = checkBoxVisibility;
    }

    @WorkerThread
    public void setFolder(FileHandle folder){
        if(assembleTask==null){
            assembleTask=new AssembleTask();
        }
    }

    public void addFilter(FileHandleFilter... filters){
        this.filters=filters;
    }

    public void removeFilter(){
        this.filters=null;
    }


    /**
        @Description：注册文件夹改变的回调类，不直接使用，通过父控制器使用
    */
    public void registerFolderChangedResponder(DisplayFolderChangeResponder responder){
        if(!responders.contains(responder)){
            responders.add(responder);
        }
    }


    private void dispatchFolderChangedEventToResponders(){
        for (DisplayFolderChangeResponder responder:responders
        ) {
            responder.respondTo(this.folder);
        }
    }



    @Override
    protected RecyclerView.ViewHolder newViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        ViewHolder viewHolder= (ViewHolder) holder;

        if(position>getItemCount()-1){
            return;
        }

        Item itemData= (Item) listOfItems.get(position);
        if(itemData==null)
            return;

        viewHolder.iv_icon.setImageResource(itemData.getDrawableId());
        viewHolder.tv_title.setText(itemData.getTitle());
        viewHolder.checkBox.setChecked(itemData.isItemChecked());
        viewHolder.checkBox.setVisibility(this.checkBoxVisibility);
        if(viewType==LINEAR){
            viewHolder.tv_sub_title.setText(itemData.getSubTitle());
        }
    }


    public static class Item implements AbsRecyclerLinearAdapter.ItemData{

        String title;
        String subtitle;
        boolean isChecked=false;
        int iconId;

        String getTitle(){
            return title;
        }

        String getSubTitle(){
            return subtitle;
        }

        boolean isItemChecked(){
            return isChecked;
        }

        void setCheck(boolean check){
            isChecked=check;
        }

        int getDrawableId(){
            return iconId;
        }
    }

    protected class ViewHolder extends RecyclerView.ViewHolder{

        ImageView iv_icon;
        CheckBox checkBox;
        TextView tv_title;
        TextView tv_sub_title;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iv_icon=itemView.findViewById(R.id.viewHolder_imgView);
            tv_title=itemView.findViewById(R.id.viewHolder_textViewTitle);
            checkBox=itemView.findViewById(R.id.checkbox);
            if(viewType==LINEAR){
                tv_sub_title=itemView.findViewById(R.id.viewHolder_textViewSubTitle);
            }
        }
    }

    protected class AssembleTask implements Runnable{



        @Override
        public void run() {
            synchronized (listOfItems) {
                listOfItems.clear();

                List<FileHandle> list_of_files = Arrays.asList(folder.listFiles());

                for(int i=0;i<list_of_files.size();i++){
                    FileHandle handle=list_of_files.get(i);

                    if(filters!=null){
                        boolean accept=true;
                        for(int j=0;j<filters.length;j++){
                            if(!filters[j].accept(handle)){
                                accept=false;
                                break;
                            }
                        }
                        if(!accept)
                            continue; //skip this item
                    }

                    Item item=new Item();
                    item.title=handle.getName();
                    if(viewType==LINEAR){
                        StringBuilder detailInfo=new StringBuilder();
                        detailInfo.append("  size: " + FMFormatter.getSuitableFileSizeString(handle.Size()));
                        detailInfo.append(" " + FMFormatter.timeDescriptionConvert_LongStyle_l2s(handle.getLastModifiedTime()));

                        if (handle.isDirectory()) {
                            item.iconId = R.drawable.icon_default_dir;
                            detailInfo.insert(0,"Total: " + handle.getFileCount()+" ");
                        } else {
                            item.iconId=IconResourceIdHelper.getIconResourceIdByFileName(item.title);
                        }

                        item.subtitle=detailInfo.toString();
                    }
                    else{
                        if(handle.isDirectory()){
                            item.iconId=R.drawable.icon_default_dir;
                        }
                        else{
                            item.iconId=IconResourceIdHelper.getIconResourceIdByFileName(item.title);
                        }
                    }
                    listOfItems.add(item);
                }
            }
            //dispatch folder change response event
            dispatchFolderChangedEventToResponders();
        }
    }

}
