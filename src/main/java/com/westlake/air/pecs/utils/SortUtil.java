package com.westlake.air.pecs.utils;

import com.google.common.collect.Ordering;
import com.westlake.air.pecs.domain.db.ScoresDO;

import java.util.Comparator;
import java.util.List;

public class SortUtil {

    public static List<ScoresDO> sortByPeptideRef(List<ScoresDO> scores){
        Ordering<ScoresDO> ordering = Ordering.from(new Comparator<ScoresDO>() {
            @Override
            public int compare(ScoresDO o1, ScoresDO o2) {
               return o1.getPeptideRef().compareTo(o2.getPeptideRef());
            }
        });

        return ordering.sortedCopy(scores);
    }

}
