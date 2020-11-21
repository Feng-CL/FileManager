package com.scut.filemanager.util;
import android.util.Log;

import java.util.LinkedList;

public class SimpleArrayFilter<E>{

    public E[] filter(E[] inputElements,Filter<E> eFilter){

        LinkedList<E> linkedList=new LinkedList<E>();
        for(int i=0;i<inputElements.length;i++){
            if(eFilter.accept(inputElements[i])){
                linkedList.add(inputElements[i]);
            }
        }
        try {
            E[] elementFilter = (E[])(new Object[linkedList.size()]);
            linkedList.toArray(elementFilter);
            return elementFilter;
        }
        //处理类转换异常
        catch (ClassCastException castException){
            Log.e(this.getClass().getName(),castException.getMessage());
            return null;
        }

    }
}
