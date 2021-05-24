package dev.anhcraft.radiumenu.gui.pagination;

public interface ElementCallback<T> {
    void supply(int count, int index, T element);
}
