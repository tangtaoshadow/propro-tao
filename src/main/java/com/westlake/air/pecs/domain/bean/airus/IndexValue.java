package com.westlake.air.pecs.domain.bean.airus;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
public class IndexValue implements Comparable<IndexValue> {

    double value;
    int index;

    public IndexValue(double value, int index) {
        this.value = value;
        this.index = index;
    }

    public static List<IndexValue> buildList(Double[] arrays) {
        if (arrays == null || arrays.length == 0) {
            return null;
        }

        List<IndexValue> indexValues = new ArrayList<>();
        for (int i = 0; i < arrays.length; i++) {
            indexValues.add(new IndexValue(arrays[i], i));
        }
        return indexValues;
    }

    @Override
    public int compareTo(@NotNull IndexValue o) {

        if (value < o.value) {
            return -1;
        }
        if (value > o.value) {
            return 1;
        }
        if(value == o.value){
            return 0;
        }
        return 0;
    }

}
