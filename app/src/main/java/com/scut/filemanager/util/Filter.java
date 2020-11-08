package com.scut.filemanager.util;

public interface Filter<E> {
    public boolean accept(E e);
}
