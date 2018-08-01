package com.westlake.air.pecs.algorithm;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.westlake.air.pecs.constants.ResidueType;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.*;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.TransitionQuery;
import com.westlake.air.pecs.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.westlake.air.pecs.constants.Constants.MAX_PAGE_SIZE_FOR_FRAGMENT;

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
        String sequence = transitionDO.getSequence();
        Annotation annotation = transitionDO.getAnnotation();
        fragment.setUnimodMap(transitionDO.getUnimodMap());

        String fs = getFragmentSequence(sequence, annotation.getType(), annotation.getLocation());

        String type = annotation.getType();
        if (type.equals(ResidueType.AIon) || type.equals(ResidueType.BIon) || type.equals(ResidueType.CIon)) {
            fragment.setStart(0);
            //因为location是从1开始计数的,而这边的end是从0开始计数的
            fragment.setEnd(annotation.getLocation() - 1);
        } else if (type.equals(ResidueType.XIon) || type.equals(ResidueType.YIon) || type.equals(ResidueType.ZIon)) {
            fragment.setStart(sequence.length() - annotation.getLocation());
            fragment.setEnd(sequence.length() - 1);
        } else if (type.equals(ResidueType.Full)) {
            fragment.setStart(0);
            fragment.setEnd(sequence.length() - 1);
        }
        fragment.setSequence(fs);
        fragment.setIsotope(annotation.isIsotope());
        fragment.setLocation(annotation.getLocation());
        fragment.setDeviation(annotation.getDeviation());
        fragment.setAdjust(annotation.getAdjust());
        fragment.setType(annotation.getType());
        fragment.setCharge(annotation.getCharge());
        fragment.setMonoMz(formulaCalculator.getMonoMz(fragment));
        fragment.setAverageMz(formulaCalculator.getAverageMz(fragment));
        fragments.add(fragment);

        return fragments;
    }

    /**
     * 获取不包含计算分子质量数据的fragment,也不包含修饰基团
     *
     * @param transitionDO
     * @return
     */
    public List<Fragment> getBaseFragments(TransitionDO transitionDO) {
        List<Fragment> fragments = new ArrayList<>();
        Fragment fragment = new Fragment();
        String sequence = transitionDO.getSequence();
        Annotation annotation = transitionDO.getAnnotation();
        fragment.setUnimodMap(transitionDO.getUnimodMap());

        String fs = getFragmentSequence(sequence, annotation.getType(), annotation.getLocation());

        fragment.setIsotope(annotation.isIsotope());
        fragment.setDeviation(annotation.getDeviation());
        fragment.setLocation(annotation.getLocation());
        fragment.setAdjust(annotation.getAdjust());
        fragment.setSequence(fs);
        fragment.setType(annotation.getType());
        fragment.setCharge(annotation.getCharge());
        fragments.add(fragment);

        return fragments;
    }

    public String getFragmentSequence(String originSequence, String type, int location) {
        if (type.equals(ResidueType.AIon) || type.equals(ResidueType.BIon) || type.equals(ResidueType.CIon)) {
            return originSequence.substring(0, location);
        } else if (type.equals(ResidueType.XIon) || type.equals(ResidueType.YIon) || type.equals(ResidueType.ZIon)) {
            return originSequence.substring(originSequence.length() - location, originSequence.length());
        } else if (type.equals(ResidueType.Full)) {
            return originSequence;
        } else {
            logger.error("解析出未识别离子类型:" + type);
            return null;
        }
    }

    public List<AminoAcid> getFragmentSequence(List<AminoAcid> originList, String type, int location) {
        if (type.equals(ResidueType.AIon) || type.equals(ResidueType.BIon) || type.equals(ResidueType.CIon)) {
            return originList.subList(0, location);
        } else if (type.equals(ResidueType.XIon) || type.equals(ResidueType.YIon) || type.equals(ResidueType.ZIon)) {
            return originList.subList(originList.size() - location, originList.size());
        } else if (type.equals(ResidueType.Full)) {
            return originList;
        } else {
            logger.error("解析出未识别离子类型:" + type);
            return null;
        }
    }

    public FragmentResult decoyOverview(String libraryId) {
        FragmentResult result = new FragmentResult();
        logger.info("数据库读取数据");

        TransitionQuery query = new TransitionQuery();
        query.setLibraryId(libraryId);
        query.setPageSize(MAX_PAGE_SIZE_FOR_FRAGMENT);
        long totalCount = transitionService.count(query);
        int totalPage = 1;
        if (totalCount > MAX_PAGE_SIZE_FOR_FRAGMENT) {
            totalPage = (int) (totalCount / MAX_PAGE_SIZE_FOR_FRAGMENT) + 1;
        }

        HashSet<Fragment> targetFragments = new HashSet<>();
        HashSet<Fragment> decoyFragments = new HashSet<>();
        int countDecoy = 0;
        int countTarget = 0;

        for (int i = 1; i <= totalPage; i++) {
            query.setPageNo(i);
            ResultDO<List<TransitionDO>> resultTmp = transitionService.getList(query);
            logger.info("读取第" + i + "批数据,总计" + totalPage + "批");
            List<TransitionDO> list = resultTmp.getModel();

            for (TransitionDO transition : list) {
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
        }

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

    public List<AminoAcid> parseAminoAcid(String sequence, HashMap<Integer,String> unimodMap){

        List<AminoAcid> aminoAcids = new ArrayList<>();
        char[] sequenceArray = sequence.toCharArray();
        for (int i = 0; i < sequenceArray.length; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(sequenceArray[i]));
            if (unimodMap != null) {
                aa.setModId(unimodMap.get(i));
            }
            aminoAcids.add(aa);
        }
        return aminoAcids;
    }

    public List<MzResult> check(String libraryId, Double threshold, boolean isDecoy) {
        if (threshold == null) {
            threshold = 0.05;
        }

        logger.info("数据库读取数据");
        TransitionQuery query = new TransitionQuery();
        query.setLibraryId(libraryId);
        query.setIsDecoy(isDecoy);
        query.setPageSize(MAX_PAGE_SIZE_FOR_FRAGMENT);
        long totalCount = transitionService.count(query);
        int totalPage = 1;
        if (totalCount > MAX_PAGE_SIZE_FOR_FRAGMENT) {
            totalPage = (int) (totalCount / MAX_PAGE_SIZE_FOR_FRAGMENT) + 1;
        }

        HashSet<Fragment> fragments = new HashSet<>();
        List<MzResult> mzResultList = new ArrayList<>();
        int count = 0;
        int countForError = 0;
        for (int i = 1; i <= totalPage; i++) {
            query.setPageNo(i);
            ResultDO<List<TransitionDO>> resultDO = transitionService.getList(query);
            logger.info("读取第" + i + "批数据,总计" + totalPage + "批");
            List<TransitionDO> list = resultDO.getModel();

            for (TransitionDO transition : list) {
                List<Fragment> tmp = getFragments(transition);
                if (tmp != null && tmp.size() > 0) {
                    for (Fragment fragment : tmp) {
                        count++;
                        if (!fragments.contains(fragment)) {
                            count++;
                            fragments.add(fragment);
                            double result = Math.abs(transition.getProductMz() - fragment.getMonoMz());
                            if (result > threshold) {
                                MzResult mzResult = new MzResult();
                                mzResult.setOriginMz(transition.getProductMz());
                                mzResult.setNewMz(fragment.getMonoMz());
                                mzResult.setDelta(result);
                                mzResult.setCharge(fragment.getCharge());
                                mzResult.setSequence(transition.getSequence());
                                mzResult.setType(fragment.getType());
                                mzResult.setOriginSequence(transition.getFullName());
                                mzResult.setFragmentSequence(fragment.getSequence());
                                mzResult.setAnnotations(transition.getAnnotations());
                                mzResult.setPrecursorCharge(transition.getPrecursorCharge());
                                mzResult.setPrecursorMz(transition.getPrecursorMz());
                                mzResult.setNewPrecursorMz(formulaCalculator.getMonoMz(transition));
                                mzResult.setLocation(fragment.getLocation());
                                if (Math.abs(transition.getPrecursorMz() - mzResult.getNewPrecursorMz()) > threshold) {
                                    mzResult.setDelatPrecursorMz(Math.abs(transition.getPrecursorMz() - mzResult.getNewPrecursorMz()));
                                }
                                mzResultList.add(mzResult);

                                countForError++;
                            }
                        }

                    }
                }
            }
        }
        logger.info("总计Fragment条数:" + count + ";阈值为:" + threshold + ";其中超过阈值的有" + countForError + "条");
        return mzResultList;
    }
}
