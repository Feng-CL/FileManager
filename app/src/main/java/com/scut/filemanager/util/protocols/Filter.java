package com.scut.filemanager.util.protocols;

public interface Filter<E> {
    public boolean accept(E e);
}
