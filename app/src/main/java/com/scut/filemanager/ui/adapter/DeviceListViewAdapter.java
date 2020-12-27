package com.scut.filemanager.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.scut.filemanager.R;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class DeviceListViewAdapter extends BaseAdapter {
    //data models
    private List<ItemData>  table =new ArrayList<>(5);
    //inflater
    private LayoutInflater inflater;

    public DeviceListViewAdapter(@NonNull LayoutInflater layoutInflater){
        inflater=layoutInflater;
    }

    @Override
    public int getCount() {
        return table.size();
    }

    @Override
    public Object getItem(int i){
        if(i> table.size()||i<0){
            return null;
        }
        return table.get(i);
    }


    /*
        @Description:This function will return -1 if i is not effective
    */


    @Override
    public long getItemId(int i) {
        if(i> table.size()||i<0){
            return -1;
        }
        return table.get(i).id;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parentView) {
        if(getCount()==0) {
            return null;
        }

        if(convertView==null){
            //attachToRoot set to false, true means return root;
            convertView=inflater.inflate(R.layout.list_view_item_for_device,parentView,false);
        }

        TextView textView_DeviceName=convertView.findViewById(R.id.textview_device_title);
        TextView textView_DeviceIp=convertView.findViewById(R.id.textview_device_ip);

        ItemData item= (ItemData) getItem(i);
        textView_DeviceName.setText(item.DeviceName);
        textView_DeviceIp.setText(item.DeviceIp);

        CheckBox checkBox=convertView.findViewById(R.id.checkbox_for_device);
        checkBox.setChecked(item.isSelected);

        return convertView;

    }

    /*
        @Description:这将清空先前的ItemsData
        @Params:list
    */

    synchronized public boolean updateItemsWith(List<ItemData> itemsList){
        table.clear();
        return table.addAll(itemsList);
    }

    synchronized public boolean addItemsWith(List<ItemData> itemsList){
        return table.addAll(itemsList);
    }

    synchronized public void addItems(DeviceListViewAdapter.ItemData... items){
        for (int i = 0; i < items.length; i++) {
            if(!this.contain(items[i])){
                this.table.add(items[i]);
            }
        }
    }

    synchronized public void clearItems(){
        table.clear();
    }

    public boolean contain(ItemData item){
        Iterator<ItemData> iterItem=table.iterator();
        while(iterItem.hasNext()){
           if(iterItem.next().id==item.id){
               return true;
           }
        }
        return false;
    }

    public static class ItemData{
        public boolean isSelected=false;
        public String DeviceName="unknown";
        public String DeviceIp="";
        public int id;

        public ItemData(int Id){
            id=Id;
        }
    }


}
