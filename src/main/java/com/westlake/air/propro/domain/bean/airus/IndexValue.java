package com.westlake.air.propro.domain.bean.airus;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
public class IndexValue<T extends Comparable> implements Comparable<IndexValue<T>> {

    int index;
    T value;

    public IndexValue(){

    }

    public IndexValue(int index, T value) {
        this.value = value;
        this.index = index;
    }

    public List<IndexValue<T>> buildList(T[] arrays) {
        if (arrays == null || arrays.length == 0) {
            return null;
        }

        List<IndexValue<T>> indexValues = new ArrayList<>();
        for (int i = 0; i < arrays.length; i++) {
            indexValues.add(new IndexValue<>(i, arrays[i]));
        }
        return indexValues;
    }

    @Override
    public int compareTo(@NotNull IndexValue<T> o) {

        if (value.compareTo(o.value) < 0) {
            return -1;
        }
        if (value.compareTo(o.value) > 0) {
            return 1;
        }
        if(value == o.value){
            return 0;
        }
        return 0;
    }

}
