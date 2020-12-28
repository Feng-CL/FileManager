package com.scut.filemanager.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.scut.filemanager.R;
import com.scut.filemanager.core.FileHandle;
import com.scut.filemanager.core.Service;
import com.scut.filemanager.util.protocols.DisplayFolderChangeResponder;


public class LocationBarController extends BaseController implements View.OnClickListener, DisplayFolderChangeResponder {
    private String[] canonical_path_tokens;
    private FileHandle folder;
    private HorizontalScrollView scrollView;
    private LinearLayout layout_container;
    private TabViewController parent_controller;

    Button rootBtn;
    TextView separatorTextView;


    public LocationBarController(FileHandle folder, ViewStub stub,TabViewController parentController){
        //inflate the layout
        super();
        stub.setLayoutResource(R.layout.location_bar);
        scrollView=(HorizontalScrollView) stub.inflate();
        stub.setVisibility(View.VISIBLE);
        layout_container=scrollView.findViewById(R.id.linearLayout_locationBar_container);

        rootBtn=layout_container.findViewById(R.id.btn_displayFolderName_borderless);
        separatorTextView=layout_container.findViewById(R.id.textview_separator_basic);

        setFolderAndUpdateView(folder);
        parent_controller=parentController;

    }

    protected void setUpHandler(){
        this.mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch(msg.what){
                    case MessageCode.SCROLL_TO_END:
                        //scroll this location bar to the end, when view is not ready this method will fail
                        //scrollView.scrollTo(layout_container.getWidth()+300,scrollView.getScrollY());
                        scrollView.smoothScrollTo(layout_container.getWidth()+300,scrollView.getScrollY());
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        };
    }


    public void setFolderAndUpdateView(FileHandle folder){
        this.folder=folder;
        if(this.folder!=null){
            spiltTokens();
            //rebuild location bar
            layout_container.removeViews(2,layout_container.getChildCount()-2); //这一步可以优化
            int btnCount=canonical_path_tokens.length;

            for (int i = 1; i < btnCount; i++) {
                Button btn = new Button(layout_container.getContext());

                btn.setLayoutParams(rootBtn.getLayoutParams());
                btn.setTextColor(rootBtn.getTextColors());
                btn.setText(canonical_path_tokens[i]);
                btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                Log.d("LocationBar", "setFolderAndUpdateView: compareTextSize" + btn.getTextSize() + " : " + rootBtn.getTextSize());
                btn.setBackground(rootBtn.getBackground());
                btn.setAllCaps(false);
                btn.setTag(i);
                btn.setMinimumWidth(5); //View
                btn.setMinWidth(5); //TextView
                btn.setOnClickListener(this);
                layout_container.addView(btn);

                if(i!=btnCount-1) {
                    TextView separator_textView = new TextView(layout_container.getContext());
                    separator_textView.setLayoutParams(separatorTextView.getLayoutParams());
                    separator_textView.setText(separatorTextView.getText());
                    separator_textView.setTextAlignment(separatorTextView.getTextAlignment());
                    separator_textView.setTextColor(separatorTextView.getTextColors());
                    layout_container.addView(separator_textView);
                }
                else{
                    btn.setTextColor(layout_container.getResources().getColor(R.color.pureBlack));
                }

                mHandler.sendEmptyMessage(MessageCode.SCROLL_TO_END);


            }

        }


    }


    private void spiltTokens(){
        if(folder!=null){
            folder.tryRetrieveCanonicalPath();
            String CanonicalPath=folder.getCanonicalPathName();
            if(CanonicalPath!=null){
                canonical_path_tokens=CanonicalPath.split("/");
            }
        }
    }

    @Override
    public void onClick(View view) {
        if(view instanceof Button) {
            int tag_on_btn = (int)view.getTag();
            FileHandle folder=getLocationFileHandleByTag(tag_on_btn);
            if(folder!=null){
                parent_controller.setDisplayFolder(folder);
            }
        }
    }

    private FileHandle getLocationFileHandleByTag(int tag){
        if(tag>0&&tag<canonical_path_tokens.length){ //this will not handle "/" button
            StringBuilder pathname=new StringBuilder();
            for (int i = 1; i <= tag; i++) {
                pathname.append("/"+canonical_path_tokens[i]);
            }
            FileHandle file=new FileHandle(pathname.toString());
            if(file.isExist()){
                return file;
            }
            else{
                return null;
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void respondTo(FileHandle folder) {
        setFolderAndUpdateView(folder);
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public Handler getHandler() {
        return this.mHandler;
    }

    @Override
    public Service getFileManagerCoreService() {
        return parent_controller.getFileManagerCoreService();
    }

    static public class MessageCode{
        static public final int SCROLL_TO_END=0;
    }
}
