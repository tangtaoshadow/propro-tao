package com.westlake.air.propro.utils;

import com.google.common.collect.Ordering;
import com.westlake.air.propro.domain.bean.analyse.WindowRang;
import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.simple.SimpleScores;

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
                    try{
                        return o1.getMainScore().compareTo(o2.getMainScore());
                    }catch (Exception e){
                        e.printStackTrace();
                        return 0;
                    }
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

    /**
     * @param overviews
     * @param isDesc 是否降序排序
     * @return
     */
    public static List<AnalyseOverviewDO> sortByMatchedPeptideCount(List<AnalyseOverviewDO> overviews, boolean isDesc) {
        Ordering<AnalyseOverviewDO> ordering = Ordering.from(new Comparator<AnalyseOverviewDO>() {
            @Override
            public int compare(AnalyseOverviewDO o1, AnalyseOverviewDO o2) {
                if (isDesc) {
                    return o2.getMatchedPeptideCount().compareTo(o1.getMatchedPeptideCount());
                } else {
                    return o1.getMatchedPeptideCount().compareTo(o2.getMatchedPeptideCount());
                }
            }
        });

        return ordering.sortedCopy(overviews);
    }

    /**
     * @param peptides
     * @param isDesc 是否降序排序
     * @return
     */
    public static List<PeptideDO> sortByMz(List<PeptideDO> peptides, boolean isDesc) {
        Ordering<PeptideDO> ordering = Ordering.from(new Comparator<PeptideDO>() {
            @Override
            public int compare(PeptideDO o1, PeptideDO o2) {
                if (isDesc) {
                    return o2.getMz().compareTo(o1.getMz());
                } else {
                    return o1.getMz().compareTo(o2.getMz());
                }
            }
        });

        return ordering.sortedCopy(peptides);
    }

    /**
     * @param rangs
     * @param isDesc 是否降序排序
     * @return
     */
    public static List<WindowRang> sortByMzStart(List<WindowRang> rangs, boolean isDesc) {
        Ordering<WindowRang> ordering = Ordering.from(new Comparator<WindowRang>() {
            @Override
            public int compare(WindowRang o1, WindowRang o2) {
                if (isDesc) {
                    return o2.getMzStart().compareTo(o1.getMzStart());
                } else {
                    return o1.getMzStart().compareTo(o2.getMzStart());
                }
            }
        });

        return ordering.sortedCopy(rangs);
    }

}
