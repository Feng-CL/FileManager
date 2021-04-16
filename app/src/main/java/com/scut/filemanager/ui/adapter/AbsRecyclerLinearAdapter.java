package com.scut.filemanager.ui.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.scut.filemanager.ui.transaction.MessageBuilder;

import java.util.List;

/**
 * 这是一个比较通用的适配器，可以根据需要，继承该对象。
 * ViewHolder 相当于循环视图里每一项的视图持有对象
 */
abstract public class AbsRecyclerLinearAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected List<ItemData> listOfItems=null;
    protected int viewHolderLayoutId;

    protected Handler mHandler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case UIMessageCode.NOTIFY_ALL:
                    notifyDataSetChanged();
                    break;
                case UIMessageCode.NOTIFY_CHANGE:
                    //暂时看看能不能在另一端更改信息
                    break;
                case UIMessageCode.NOTIFY_REMOVE:
                    notifyItemRemoved(msg.arg1);
                    break;
                case UIMessageCode.NOTIFY_INSERT:
                    notifyItemInserted(msg.arg1);
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    final static class UIMessageCode{
        final static int NOTIFY_REMOVE=0;
        final static int NOTIFY_ALL=1;
        final static int NOTIFY_INSERT=2; //arg1=position
        final static int NOTIFY_CHANGE=3; //arg1=position
    }

    public AbsRecyclerLinearAdapter(int viewHolderLayoutId){
        this.viewHolderLayoutId=viewHolderLayoutId;
    }

    public AbsRecyclerLinearAdapter(List<ItemData> data, int viewHolderLayoutId){
        this.listOfItems=data;
        this.viewHolderLayoutId=viewHolderLayoutId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //here ignore viewType
        Context context=parent.getContext();
        View linearLayoutView= LayoutInflater.from(context).inflate(viewHolderLayoutId,parent,false);
        return newViewHolder(linearLayoutView);
    }

    /**
     * 向该适配器中，添加单个数据，该方法可以在非ui线程中执行,以获得视图显示同步
     * @param item
     */
    public void addItem(ItemData item){
        listOfItems.add(item);
        mHandler.sendMessage(MessageBuilder.obtain(UIMessageCode.NOTIFY_INSERT,listOfItems.size()-1));
    }

    /**
     * 移除适配器中对应位置的数据项，position应该由ViewHolder.getAdapterPosition获得
     * @param position
     */
    public void removeItem(int position){
        if(position<0||position>listOfItems.size()-1)
            return; //out of range
        listOfItems.remove(position);
        mHandler.sendMessage(MessageBuilder.obtain(UIMessageCode.NOTIFY_REMOVE,position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return listOfItems.size();
    }

    /**
     * 数据获取协议，进一步解耦
     */
    public interface ItemData{

    }

    abstract protected RecyclerView.ViewHolder newViewHolder(View itemView);
//    public static class ViewHolderProxy extends RecyclerView.ViewHolder{
//
//        View mView;
//        ImageView imageView_icon;
//        TextView textView_title;
//        TextView textView_detailText;
//        ImageSourceIndicator imageSourceIndicator=ImageSourceIndicator.INDIRECT;
//        CheckBox checkBox;
//        int positionBound;
//
//
//        public ViewHolderProxy(@NonNull View itemView) {
//            super(itemView);
//            mView=itemView;
//            imageView_icon=mView.findViewById(imgId);
//            textView_title=mView.findViewById(textViewTitleId);
//            textView_detailText=mView.findViewById(textViewSubTitleId);
//            checkBox=mView.findViewById(R.id.checkbox);
//        }
//
//        public void setImageSourceIndicator(ImageSourceIndicator arg){
//            imageSourceIndicator=arg;
//        }
//    }
//

}
