package com.westlake.air.swathplatform.algorithm;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.westlake.air.swathplatform.constants.ResidueType;
import com.westlake.air.swathplatform.domain.bean.Annotation;
import com.westlake.air.swathplatform.domain.bean.Fragment;
import com.westlake.air.swathplatform.domain.bean.FragmentResult;
import com.westlake.air.swathplatform.domain.bean.MzResult;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 10:32
 */
@Component
public class FragmentCalculator {

    public final Logger logger = LoggerFactory.getLogger(FragmentCalculator.class);

    @Autowired
    TransitionService transitionService;

    @Autowired
    FormulaCalculator formulaCalculator;

    public List<Fragment> getFragments(TransitionDO transitionDO) {
        List<Fragment> fragments = new ArrayList<>();
        Fragment fragment = new Fragment(transitionDO.getId());
        String sequence = transitionDO.getPeptideSequence();
        List<Annotation> annotationList = transitionDO.getAnnotations();
        for (Annotation annotation : annotationList) {
            String fs = getFragmentSequence(sequence, annotation.getType(), annotation.getLocation());
            if (fs == null) {
                continue;
            }

            fragment.setSequence(fs);
            fragment.setType(annotation.getType());
            fragment.setCharge(annotation.getCharge());
            fragment.setMonoWeight(formulaCalculator.getMonoMz(fragment));
            fragment.setAverageWeight(formulaCalculator.getAverageMz(fragment));
            fragments.add(fragment);
        }

        return fragments;
    }

    /**
     * 获取不包含计算分子质量数据的fragment
     *
     * @param transitionDO
     * @return
     */
    public List<Fragment> getBaseFragments(TransitionDO transitionDO) {
        List<Fragment> fragments = new ArrayList<>();
        Fragment fragment = new Fragment();
        String sequence = transitionDO.getPeptideSequence();
        List<Annotation> annotationList = transitionDO.getAnnotations();
        for (Annotation annotation : annotationList) {
            String fs = getFragmentSequence(sequence, annotation.getType(), annotation.getLocation());
            if (fs == null) {
                continue;
            }
            fragment.setSequence(fs);
            fragment.setType(annotation.getType());
            fragment.setCharge(annotation.getCharge());
            fragments.add(fragment);
        }

        return fragments;
    }

    public String getFragmentSequence(String originSequence, String type, int location) {
        if (type.equals(ResidueType.AIon) ||
                type.equals(ResidueType.BIon) ||
                type.equals(ResidueType.CIon)) {
            return originSequence.substring(0, location);
        } else if (type.equals(ResidueType.XIon) ||
                type.equals(ResidueType.YIon) ||
                type.equals(ResidueType.ZIon)) {
            return originSequence.substring(originSequence.length() - location, originSequence.length());
        } else {
            logger.error("解析出未识别离子类型:" + type);
            return null;
        }
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

        logger.info("总计有原始肽段片段:" + targetFragments.size() + "/" + countTarget + "条,占比:" + (double) targetFragments.size() / countTarget);
        logger.info("总计有伪肽段片段:" + decoyFragments.size() + "/" + countDecoy + "条,占比:" + (double) decoyFragments.size() / countDecoy);
        logger.info("开始比对");

        HashSet<Fragment> overlapFragments = new HashSet<>();
        ArrayList<Fragment> targetListTemp = new ArrayList<>(targetFragments);
        int count = 0;
        int countForTarget = 0;

        for (Fragment decoy : decoyFragments) {
            if (targetFragments.contains(decoy)) {
                countForTarget += targetListTemp.get(targetListTemp.indexOf(decoy)).getCount();
                overlapFragments.add(decoy);
                count += decoy.getCount();
            }
        }

        Multiset<Integer> countForTargetSet = HashMultiset.create();
        Multiset<Integer> countForDecoySet = HashMultiset.create();
        for (Fragment fragment : targetFragments) {
            countForTargetSet.add(fragment.getCount());
        }

        for (Fragment fragment : decoyFragments) {
            countForDecoySet.add(fragment.getCount());
        }

        for (int i : countForTargetSet.elementSet()) {
            System.out.println("Target:重复" + i + "次的有" + countForTargetSet.count(i) + "个");
        }
        for (int j : countForDecoySet.elementSet()) {
            System.out.println("Decoy:重复" + j + "次的有" + countForDecoySet.count(j) + "个");
        }

        Ordering<Fragment> ordering = Ordering.natural();

        ArrayList<Fragment> decoyList = new ArrayList<>(decoyFragments);
        ArrayList<Fragment> targetList = new ArrayList<>(targetFragments);
        ArrayList<Fragment> overlapList = new ArrayList<>(overlapFragments);

        logger.info("总计有" + overlapFragments.size() + "个伪肽段片段是重复的,重复了" + count + "次");
        logger.info("重复的伪肽段片段在Target片段列表中总计出现了" + countForTarget + "次");
        result.setMsgInfo("总计有原始肽段片段:" + targetFragments.size() + "/" + countTarget + "条,占比:" + (double) targetFragments.size() / countTarget + ";有伪肽段片段:" + decoyFragments.size() + "/" + countDecoy + "条,占比:" + (double) decoyFragments.size() / countDecoy + ";有" + overlapFragments.size() + "个伪肽段片段是重复的,重复了" + count + "次");
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

    public List<MzResult> checkDecoy(String libraryId) {
        logger.info("数据库读取数据");
        List<TransitionDO> transitionList = transitionService.getAllByLibraryIdAndIsDecoy(libraryId, true);

        HashSet<Fragment> decoyFragments = new HashSet<>();
        logger.info("数据读取完毕,开始初始化肽段片段");
        List<MzResult> mzResultList = new ArrayList<>();
        int count = 0;
        int countForError = 0;
        for (TransitionDO transition : transitionList) {
            List<Fragment> tmp = getBaseFragments(transition);
            if (tmp != null && tmp.size() > 0) {
                for (Fragment fragment : tmp) {
                    if (transition.getIsDecoy()) {
                        if (!decoyFragments.contains(fragment)) {
                            count++;
                            decoyFragments.add(fragment);
                            double monoMz = formulaCalculator.getMonoMz(fragment);
                            double result = Math.abs(transition.getProductMz() - monoMz);
                            if (result > 0.1) {
                                MzResult mzResult = new MzResult();
                                mzResult.setOriginMz(transition.getProductMz());
                                mzResult.setNewMz(monoMz);
                                mzResult.setDelta(result);
                                mzResult.setCharge(fragment.getCharge());
                                mzResult.setSequence(transition.getPeptideSequence());
                                mzResult.setType(fragment.getType());
                                mzResult.setOriginSequence(transition.getFullUniModPeptideName());
                                mzResult.setFragmentSequence(fragment.getSequence());
                                mzResult.setAnnotations(transition.getAnnotation());
                                mzResult.setPrecursorCharge(transition.getPrecursorCharge());
                                mzResultList.add(mzResult);

                                countForError++;
                            }
                        }
                    }
                }
            }
        }
        logger.info("总计Fragment条数:"+count+";其中超过阈值的有"+countForError+"条");
        return mzResultList;
    }
}
