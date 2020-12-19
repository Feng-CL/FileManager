package com.scut.filemanager.util;
import android.util.Log;

import com.scut.filemanager.util.protocols.Filter;

import java.util.LinkedList;

public class SimpleArrayFilter{

     static public <E> E[] filter(E[] inputElements, Filter<E> eFilter){

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
            Log.e(SimpleArrayFilter.class.getName(),castException.getMessage());
            return null;
        }

    }




    static public <E> boolean hasElementByLinearSearch(E[] elements,E arge){
        for (E e:
            elements) {
            if(e.equals(arge)){
                return true;
            }
        }


        return false;
    }
}
