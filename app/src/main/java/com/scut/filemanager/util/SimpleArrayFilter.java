package com.scut.filemanager.util;
import java.util.LinkedList;

public class SimpleArrayFilter<E>{

    public E[] filter(E[] inputElements,Filter<E> eFilter){

        LinkedList<E> linkedList=new LinkedList<E>();
        for(int i=0;i<inputElements.length;i++){
            if(eFilter.accept(inputElements[i])){
                linkedList.add(inputElements[i]);
            }
        }
        E[] ElementsFiltered=linkedList.toArray((E[])(new Object[0]));
        return ElementsFiltered;
    }
}
