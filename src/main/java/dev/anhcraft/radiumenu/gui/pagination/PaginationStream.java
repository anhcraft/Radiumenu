package dev.anhcraft.radiumenu.gui.pagination;

import dev.anhcraft.jvmkit.helpers.PaginationHelper;
import dev.anhcraft.jvmkit.utils.CollectionUtil;

import java.util.List;

public class PaginationStream<T> {
    private T[] array;
    private final int index;
    private final int size;

    public PaginationStream(T[] data, int index, int size) {
        this.array = data;
        this.index = index;
        this.size = size;
    }

    public PaginationStream(List<T> data, int index, int size) {
        if(!data.isEmpty()) //noinspection unchecked
            this.array = CollectionUtil.toArray(data, (Class<? extends T>) data.get(0).getClass());
        this.index = index;
        this.size = size;
    }

    public int forEach(ElementCallback<T> consumer){
        if(array != null && array.length > 0) {
            PaginationHelper<T> l = new PaginationHelper<>(array, size);
            l.open(index + 1); // page starts from 1
            T[] data = l.collect();
            if (data.length != 0 || index == 0) {
                int i = 0;
                for (T u : data) {
                    consumer.supply(i, index * size + i, u);
                    i++;
                }
                return i;
            }
        }
        return 0;
    }
}
