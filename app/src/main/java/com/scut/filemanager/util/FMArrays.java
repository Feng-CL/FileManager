package com.scut.filemanager.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FMArrays {
    static public <E> List<E> asList(E[] elements){
        List<E> ls=new ArrayList<>(elements.length);
        ls.addAll(Arrays.asList(elements));
        return ls;
    }
}
