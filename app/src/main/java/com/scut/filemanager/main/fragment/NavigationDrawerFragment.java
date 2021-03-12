package com.scut.filemanager.main.fragment;


import android.animation.TimeInterpolator;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.scut.filemanager.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigationDrawerFragment extends Fragment {

    //UI Outlet
    protected View mView;
    protected ExpandableListView expandableListView;
    protected ImageView imageView_header;
    protected TextView textView_header;

    //measure margin
    protected int marginLeft;
    protected int width;

    //private Handler mHandler=new Handler(Looper.getMainLooper()); //need mainLooper

    private Handler mHandler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case UIMessageCode.OTHER_UI_CONFIGURATION_PROCESS:
                    otherUIConfigurationProcess();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };


    final static private class UIMessageCode{
        final static int OTHER_UI_CONFIGURATION_PROCESS=1;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView=inflater.inflate(R.layout.navigation_fragment_layout,container,false);
        //get ui element from mView here
        expandableListView=mView.findViewById(R.id.expandListView_navigationFragment);
        imageView_header=mView.findViewById(R.id.imgview_nav_header);
        textView_header=mView.findViewById(R.id.textview_nav_header_title);

        marginLeft=-mView.getWidth();
        width=mView.getWidth();
        //mView.setLeft(marginLeft);
        //otherUIConfigurationProcess();
        createExpandableListView();

        mHandler.sendEmptyMessageDelayed(UIMessageCode.OTHER_UI_CONFIGURATION_PROCESS,800);
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        expandableListView.expandGroup(0);
    }

    public void pullNavigation(){
        //mView.setVisibility(View.VISIBLE);
        marginLeft=-mView.getWidth();
        width=mView.getWidth();
        mView.animate().translationX(width).setDuration(250).setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float v) {
                return (float) Math.sin(Math.PI*v/2);
            }
        });
       // boolean result=mHandler.postAtFrontOfQueue(new PullPushNavigationTask(true));
    }

    public void pushNavigation(){

        mView.animate().translationX(0).setDuration(250).setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float v) {
                return (float)Math.sin(Math.PI*v/2);
            }
        });

    }

    private void otherUIConfigurationProcess(){
        TextView bottomTextView=mView.findViewById(R.id.textview_in_switchView);
        bottomTextView.setText(R.string.show_system_hidden_file);

        int view_w=expandableListView.getWidth();
        //adjust indicatorPosition and hide child indicator (which is already done in xml file)
        expandableListView.setIndicatorBounds(view_w-75,view_w-5);
        Switch hiddenFilesSwitch=mView.findViewById(R.id.Switch);
        //hiddenFilesSwitch
        //bind checkBox to listener
    }

    public void setUpListenerForSwitch(){

    }

    public void setListenerForExpandableListView(ExpandableListView.OnChildClickListener onChildClickListener){
        if(expandableListView!=null) {
            expandableListView.setOnChildClickListener(
                onChildClickListener
            );
        }
    }

    private void createExpandableListView(){

        if(expandableListView==null)
            return;

        //仅仅能适配图片文字配合加载
        class CustomExpandableListViewAdapter extends BaseExpandableListAdapter{

            List<Map<String,Object>> groupData;
            List<? extends List<String>> childData;
            int groupLayout,childLayout;
            String[] groupKey;
            int[] groupViewId;
            String childKey;
            int childViewId;

            public CustomExpandableListViewAdapter(List<Map<String,Object>> groupData, int groupLayout,String[] groupKey, int[] groupViewId,
                                                   List<List<String>> childData,int childLayout, String childKey, int childViewId
            ){
                this.groupData=groupData;
                this.childData=childData;
                this.groupLayout=groupLayout;
                this.childLayout=childLayout;
                this.groupKey=groupKey;
                this.childKey=childKey;
                this.groupViewId=groupViewId;
                this.childViewId=childViewId;
            }

            @Override
            public int getGroupCount() {
                return groupData.size();
            }

            @Override
            public int getChildrenCount(int groupPosition) {
                if(groupPosition<getGroupCount()) {
                    return childData.get(groupPosition).size();
                }
                else{
                    return 0;
                }
            }

            @Override
            public Object getGroup(int groupPosition) {
                return groupData.get(groupPosition);
            }

            @Override
            public Object getChild(int groupPosition, int childPosition) {
                return childData.get(groupPosition).get(childPosition);
            }

            @Override
            public long getGroupId(int groupPosition) {
                return groupPosition;
            }

            @Override
            public long getChildId(int groupPosition,
                                   int childPosition) {
                //不超过10元素时，使用十进制确认孩子位置
                return groupPosition*10+childPosition;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getGroupView(int groupPosition,
                                     boolean isExpanded,
                                     View convertView,
                                     ViewGroup parent) {
                if(getGroupCount()==0||groupPosition>getGroupCount()){
                    return null; //无需装载
                }

                if(convertView==null){
                    convertView=getLayoutInflater().inflate(groupLayout,parent,false);
                }
                //ignore argument isExpanded for the moment
                Map<String,Object> groupEntry=groupData.get(groupPosition);
                ImageView icon=convertView.findViewById(groupViewId[0]);
                TextView groupTextView=convertView.findViewById(groupViewId[1]);

                icon.setImageDrawable((Drawable) groupEntry.get(groupKey[0]));
                groupTextView.setText((String)groupEntry.get(groupKey[1]));
//
//                Log.d("groupClickable:", String.valueOf(convertView.isClickable()));
//                Log.d("groupEnable:", String.valueOf(convertView.isEnabled()));
//                Log.d("groupFlags","isFocused"+convertView.isFocused());
//                Log.d("groupFlags","isFocusable"+convertView.isFocusable());
//                Log.d("groupFlags","isFocusableInTouchMode"+convertView.isFocusableInTouchMode());

                return convertView;
            }

            @Override
            public View getChildView(int groupPosition,
                                     int childPosition,
                                     boolean isLastChild,
                                     View convertView,
                                     ViewGroup parent) {
                if(getChildrenCount(groupPosition)==0||groupPosition>getGroupCount()||childPosition>getChildrenCount(groupPosition)){
                    return null;
                }
                if(convertView==null)
                    convertView=getLayoutInflater().inflate(childLayout,parent,false);
                TextView textView=convertView.findViewById(childViewId);
                textView.setText((String)childData.get(groupPosition).get(childPosition));


                /*
                here show a slice of source code about onTouchEvent()

                                        if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
                            focusTaken = requestFocus();
                        }

                if focusTaken is equal to true do performClick



                 */


                //convertView.setClickable(true);
                //check state here
//                Log.d("childClickable:", String.valueOf(convertView.isClickable()));
//                Log.d("childEnable:", String.valueOf(convertView.isEnabled()));
//                Log.d("childFlags","isFocused"+convertView.isFocused());
//                Log.d("childFlags","isFocusable"+convertView.isFocusable());
//                Log.d("childFlags","isFocusableInTouchMode"+convertView.isFocusableInTouchMode());
                //Log.d("childFlags","adapter.isEnable"+isChildSelectable());
                return convertView;

            }



            //这个方法有点坑啊
            @Override
            public boolean isChildSelectable(int groupPosition,
                                             int childPosition) {
                return true;
            }


        }

        //
        List<Map<String,Object>> groupData=new ArrayList<>();
        List<List<String>> childData=new ArrayList<>();

        String[] groupName=getResources().getStringArray(R.array.navigationGroupName);
        String[] groupKey={"icon","name"};

        TypedArray attr_array=getResources().obtainTypedArray(R.array.navigationGroupIconId);
        final int length=attr_array.length();

        Drawable[] icons=new Drawable[length];
        for(int i=0;i<length;i++){
            icons[i]=attr_array.getDrawable(i);
        }
        attr_array.recycle();

        for (int i=0;i<groupName.length;i++) {
             Map<String,Object> groupCollection=new HashMap<>(2);
             groupCollection.put(groupKey[0],icons[i]);
             groupCollection.put(groupKey[1],groupName[i]);
             groupData.add(groupCollection);
        }

        //initialize childData
        for(int i=0;i<groupData.size();i++){
            childData.add(new ArrayList<String>());
        }

        String childKey="itemName";
        String[] children1,children2,children3,children4;
        children1=getResources().getStringArray(R.array.navigationGroupFastAccessChildren);
        children2=getResources().getStringArray(R.array.navigationGroupToolsChildren);
        children3=getResources().getStringArray(R.array.navigationGroupNetWorkChildren);
        children4=getResources().getStringArray(R.array.Library);
        childData.get(0).addAll(Arrays.asList(children1));
        childData.get(1).addAll(Arrays.asList(children4));
        childData.get(2).addAll(Arrays.asList(children2));
        childData.get(3).addAll(Arrays.asList(children3));

        CustomExpandableListViewAdapter adapter=new CustomExpandableListViewAdapter(groupData,
                R.layout.nav_group_list_item,groupKey,
                new int[]{R.id.viewItem_imgView, R.id.viewItem_textView},
                childData,
                R.layout.nav_group_child,
                childKey,
                R.id.viewItem_pureTextView);

        expandableListView.setAdapter(adapter);
    }




//
//    //out +true in -false
//    private class PullPushNavigationTask implements Runnable{
//
//        int sign;
//        int step=8;
//
//        PullPushNavigationTask(boolean inout){
//            sign= inout?1:-1;
//        }
//
//
//        @Override
//        public void run() {
//            if(marginLeft<0&&marginLeft>=-width){
//                marginLeft+=sign*step;
//                mView.setTranslationX(marginLeft+width);
//                //Log.d(this.getClass().getName(),String.valueOf(mView.getTranslationX()));
//                mHandler.postDelayed(this,1);
//            }
//        }
//    }




}
