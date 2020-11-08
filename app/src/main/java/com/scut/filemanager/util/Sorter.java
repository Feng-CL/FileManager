package com.scut.filemanager.util;
import java.util.Comparator;

public class Sorter {
    /*
    @Params: comparator serves compare(e1,e2)<=0 as positive order.
     */
    public static <E> void  mergeSort(E[] elements,Comparator<E> comparator,int Threshold){
        if(elements.length<Threshold){
            insertionSort(elements,comparator);
        }
        else{
            //a circumvent way of casting.
            /*
            In fact, Object sometimes cannot be converted to  type E when
            E's constructor doesn't accept null argument;
             */
            E[]  front_list= (E[]) new Object[elements.length/2];
            E[] back_list= (E[]) new Object[elements.length-elements.length/2];

            System.arraycopy(elements,0,front_list,0,front_list.length);
            System.arraycopy(elements,front_list.length,back_list,0,back_list.length);

            mergeSort(front_list,comparator,Threshold);
            mergeSort(back_list,comparator,Threshold);

            //merge them
            int front_index=0,back_index=0,index=0;
            while(front_index<front_list.length&&back_index<back_list.length){
                int result =comparator.compare(front_list[front_index],back_list[back_index]);
                if(result<=0){
                    elements[index++]=front_list[front_index++];
                }
                else{
                    elements[index++]=back_list[back_index++];
                }
            }
            if (front_index<front_list.length){
                System.arraycopy(front_list,front_index,elements,index,elements.length-index);
            }
            if (back_index<back_list.length){
                System.arraycopy(back_list,back_index,elements,index,elements.length-index);
            }
        }
    }


    private static <E> void insertionSort(E[] elements, Comparator<E> comparator){
        int n=elements.length-1;
        for(int i=1;i<=n;i++){
            //find the position where the element inserts
            for(int j=i;j>0;j--){
                //define the inverted order as result < 0, where result equals to comparator.compare(e1,e2)
                int result=comparator.compare(elements[j],elements[j-1]);
                if(result>0)
                    break; //here it is
                else if(result<0){ //reverted order, need to be reverted
                    E temp=elements[j-1];
                    elements[j-1]=elements[j];
                    elements[j]=temp;
                }
            }
        }
    }
}
