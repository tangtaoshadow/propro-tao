package com.westlake.air.pecs.utils;

import com.google.common.collect.Ordering;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;

import java.util.Comparator;
import java.util.List;

public class SortUtil {

    public static List<SimpleScores> sortByPeptideRef(List<SimpleScores> scores){
        Ordering<SimpleScores> ordering = Ordering.from(new Comparator<SimpleScores>() {
            @Override
            public int compare(SimpleScores o1, SimpleScores o2) {
               return o1.getPeptideRef().compareTo(o2.getPeptideRef());
            }
        });

        return ordering.sortedCopy(scores);
    }

}
