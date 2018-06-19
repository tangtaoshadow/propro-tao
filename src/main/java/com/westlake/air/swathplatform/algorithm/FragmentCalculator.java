package com.westlake.air.swathplatform.algorithm;

import com.google.common.collect.Ordering;
import com.westlake.air.swathplatform.constants.ResidueType;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.bean.Annotation;
import com.westlake.air.swathplatform.domain.bean.Fragment;
import com.westlake.air.swathplatform.domain.bean.FragmentResult;
import com.westlake.air.swathplatform.domain.bean.PeptideFormula;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 10:32
 */
@Component
public class FragmentCalculator {

    public final Logger logger = LoggerFactory.getLogger(FragmentCalculator.class);

    @Autowired
    TransitionService transitionService;

    public List<Fragment> getFragments(TransitionDO transitionDO) {
        List<Fragment> fragments = new ArrayList<>();
        Fragment fragment = new Fragment(transitionDO.getId());
        String sequence = transitionDO.getPeptideSequence();
        List<Annotation> annotationList = transitionDO.getAnnotations();
        for (Annotation annotation : annotationList) {
            PeptideFormula formula = null;
            double weight = 0;
            if (annotation.getType().equals(ResidueType.AIon) ||
                    annotation.getType().equals(ResidueType.BIon) ||
                    annotation.getType().equals(ResidueType.CIon)) {
                formula = new PeptideFormula(sequence.substring(0, annotation.getLocation()), annotation.getCharge(), annotation.getType());
            } else if (annotation.getType().equals(ResidueType.XIon) ||
                    annotation.getType().equals(ResidueType.YIon) ||
                    annotation.getType().equals(ResidueType.ZIon)) {
                formula = new PeptideFormula(sequence.substring(sequence.length() - annotation.getLocation(), sequence.length()), annotation.getCharge(), annotation.getType());
            } else {
                logger.error("解析出未识别离子类型:" + transitionDO.getAnnotation());
                return null;
            }
            fragment.setMonoWeight(formula.getMonoWeight());
            fragment.setAverageWeight(formula.getAverageWeight());
            fragments.add(fragment);
        }

        return fragments;
    }

    public List<Fragment> getBaseFragments(TransitionDO transitionDO) {
        List<Fragment> fragments = new ArrayList<>();
        Fragment fragment = new Fragment();
        String sequence = transitionDO.getPeptideSequence();
        List<Annotation> annotationList = transitionDO.getAnnotations();
        for (Annotation annotation : annotationList) {
            fragment.setType(annotation.getType());
            if (annotation.getType().equals(ResidueType.AIon) ||
                    annotation.getType().equals(ResidueType.BIon) ||
                    annotation.getType().equals(ResidueType.CIon)) {
                fragment.setSequence(sequence.substring(0, annotation.getLocation()));
            } else if (annotation.getType().equals(ResidueType.XIon) ||
                    annotation.getType().equals(ResidueType.YIon) ||
                    annotation.getType().equals(ResidueType.ZIon)) {
                fragment.setSequence(sequence.substring(sequence.length() - annotation.getLocation(), sequence.length()));
            } else {
                logger.error("解析出未识别离子类型:" + transitionDO.getAnnotation());
                return null;
            }
            fragments.add(fragment);
        }

        return fragments;
    }

    public List<String> getFragmentSequence(TransitionDO transitionDO) {

        List<String> sequences = new ArrayList<>();
        String sequence = transitionDO.getPeptideSequence();
        List<Annotation> annotationList = transitionDO.getAnnotations();
        for (Annotation annotation : annotationList) {
            if (annotation.getType().equals(ResidueType.AIon) ||
                    annotation.getType().equals(ResidueType.BIon) ||
                    annotation.getType().equals(ResidueType.CIon)) {
                sequences.add(sequence.substring(0, annotation.getLocation()));
            } else if (annotation.getType().equals(ResidueType.XIon) ||
                    annotation.getType().equals(ResidueType.YIon) ||
                    annotation.getType().equals(ResidueType.ZIon)) {
                sequences.add(sequence.substring(sequence.length() - annotation.getLocation(), sequence.length()));
            } else {
                logger.error("解析出未识别离子类型:" + transitionDO.getAnnotation());
            }
        }

        return sequences;
    }

    public FragmentResult decoyOverview(String libraryId) {
        FragmentResult result = new FragmentResult();
        logger.info("数据库读取数据");
        List<TransitionDO> transitionList = transitionService.getAllByLibraryId(libraryId);
        HashSet<Fragment> targetFragments = new HashSet<>();
        HashSet<Fragment> decoyFragments = new HashSet<>();

        int countDecoy = 0;
        int countTarget = 0;

        logger.info("数据读取完毕,开始初始化肽段片段");
        for (TransitionDO transition : transitionList) {
            List<Fragment> tmp = getBaseFragments(transition);
            if (tmp != null && tmp.size() > 0) {
                for (Fragment fragment : tmp) {
                    if (transition.getIsDecoy()) {
                        countDecoy++;
                        if (decoyFragments.contains(fragment)) {
                            fragment.count();
                        } else {
                            decoyFragments.add(fragment);
                        }
                    } else {
                        countTarget++;
                        if (targetFragments.contains(fragment)) {
                            fragment.count();
                        } else {
                            targetFragments.add(fragment);
                        }
                    }
                }
            }
        }


        logger.info("初始化肽段片段完成");

        logger.info("总计有原始肽段片段:" + targetFragments.size() + "/" + countTarget + "条,占比:"+(double)targetFragments.size()/countTarget);
        logger.info("总计有伪肽段片段:" + decoyFragments.size() + "/" + countDecoy + "条,占比:"+(double)decoyFragments.size()/countDecoy);
        logger.info("开始比对");

        HashSet<Fragment> overlapFragments = new HashSet<>();

        int count = 0;
        int countForTarget = 0;
        for (Fragment decoy : decoyFragments) {
            if (targetFragments.contains(decoy)) {
                overlapFragments.add(decoy);
                count += decoy.getCount();
//                countForTarget +=
            }
        }



        Ordering<Fragment> ordering = Ordering.natural();

        ArrayList<Fragment> decoyList = new ArrayList<>(decoyFragments);
        ArrayList<Fragment> targetList = new ArrayList<>(targetFragments);
        ArrayList<Fragment> overlapList = new ArrayList<>(overlapFragments);

        logger.info("总计有" + overlapFragments.size() + "个伪肽段片段是重复的,重复了" + count + "次");
        result.setMsgInfo("总计有原始肽段片段:" + targetFragments.size() + "/" + countTarget + "条,占比:"+(double)targetFragments.size()/countTarget+ ";有伪肽段片段:" + decoyFragments.size() + "/" + countDecoy + "条,占比:"+(double)decoyFragments.size()/countDecoy +";有" + overlapFragments.size() + "个伪肽段片段是重复的,重复了" + count + "次");
        result.setDecoyList(ordering.sortedCopy(decoyList).subList(0, decoyList.size() > 50 ? 50 : decoyList.size()));
        result.setTargetList(ordering.sortedCopy(targetList).subList(0, targetList.size() > 50 ? 50 : targetList.size()));
        result.setOverlapList(ordering.sortedCopy(overlapList).subList(0, overlapList.size() > 50 ? 50 : overlapList.size()));
        result.setDecoyTotalCount(countDecoy);
        result.setDecoyUniCount(decoyList.size());
        result.setTargetTotalCount(countTarget);
        result.setTargetUniCount(targetList.size());

        logger.info("排序完毕");
        return result;
    }
}
