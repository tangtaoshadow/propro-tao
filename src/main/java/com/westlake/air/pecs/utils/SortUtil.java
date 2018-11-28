package com.westlake.air.pecs.utils;

import com.google.common.collect.Ordering;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;

import java.util.Comparator;
import java.util.List;

public class SortUtil {

    public static List<SimpleScores> sortByPeptideRef(List<SimpleScores> scores) {
        Ordering<SimpleScores> ordering = Ordering.from(new Comparator<SimpleScores>() {
            @Override
            public int compare(SimpleScores o1, SimpleScores o2) {
                return o1.getPeptideRef().compareTo(o2.getPeptideRef());
            }
        });

        return ordering.sortedCopy(scores);
    }

    public static List<SimpleFeatureScores> sortByMainScore(List<SimpleFeatureScores> scores, boolean isDesc) {
        Ordering<SimpleFeatureScores> ordering = Ordering.from(new Comparator<SimpleFeatureScores>() {
            @Override
            public int compare(SimpleFeatureScores o1, SimpleFeatureScores o2) {
                if (isDesc) {
                    return o2.getMainScore().compareTo(o1.getMainScore());
                } else {
                    return o1.getMainScore().compareTo(o2.getMainScore());
                }
            }
        });

        return ordering.sortedCopy(scores);
    }

    /**
     * @param scores
     * @param isDesc 是否降序排序
     * @return
     */
    public static List<SimpleFeatureScores> sortByPValue(List<SimpleFeatureScores> scores, boolean isDesc) {
        Ordering<SimpleFeatureScores> ordering = Ordering.from(new Comparator<SimpleFeatureScores>() {
            @Override
            public int compare(SimpleFeatureScores o1, SimpleFeatureScores o2) {
                if (isDesc) {
                    return o2.getPValue().compareTo(o1.getPValue());
                } else {
                    return o1.getPValue().compareTo(o2.getPValue());
                }
            }
        });

        return ordering.sortedCopy(scores);
    }

}
