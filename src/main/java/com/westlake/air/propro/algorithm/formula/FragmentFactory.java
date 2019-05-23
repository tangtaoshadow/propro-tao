package com.westlake.air.propro.algorithm.formula;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResidueType;
import com.westlake.air.propro.dao.AminoAcidDAO;
import com.westlake.air.propro.dao.ElementsDAO;
import com.westlake.air.propro.dao.UnimodDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzResult;
import com.westlake.air.propro.domain.bean.score.BYSeries;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.bean.peptide.Fragment;
import com.westlake.air.propro.domain.bean.peptide.FragmentResult;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.algorithm.parser.model.chemistry.AminoAcid;
import com.westlake.air.propro.algorithm.parser.model.chemistry.Unimod;
import com.westlake.air.propro.service.PeptideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.westlake.air.propro.constants.Constants.MAX_PAGE_SIZE_FOR_FRAGMENT;
import static com.westlake.air.propro.constants.Constants.Y_SIDE_MASS;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 10:32
 */
@Component
public class FragmentFactory {

    public final Logger logger = LoggerFactory.getLogger(FragmentFactory.class);

    @Autowired
    PeptideService peptideService;
    @Autowired
    FormulaCalculator formulaCalculator;
    @Autowired
    UnimodDAO unimodDAO;
    @Autowired
    AminoAcidDAO aminoAcidDAO;
    @Autowired
    ElementsDAO elementsDAO;

    //根据UnimodMap,肽段的序列以及带电量获取该肽段所有B,Y类型的排列组合的离子MZ列表
    public BYSeries getBYSeries(HashMap<Integer, String> unimodHashMap, String sequence, int charge) {

        BYSeries bySeries = new BYSeries();

        //bSeries 若要提高精度，提高json的精度
        List<Double> bSeries = new ArrayList<>();
        double monoWeight = Constants.PROTON_MASS_U * charge;
//        if (unimodHashMap != null && unimodHashMap.containsKey(0)) {
//            Unimod unimod = unimodDAO.getUnimod(unimodHashMap.get(0));
//            if (unimod != null) {
//                monoWeight += unimod.getMonoMass();
//            }
//        }

        char[] acidCodeArray = sequence.toCharArray();
        for (int i = 0; i < acidCodeArray.length - 1; i++) {
            AminoAcid aa = aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCodeArray[i]));
            if (aa == null) {
                continue;
            }
            if (unimodHashMap != null && unimodHashMap.containsKey(i)) {
                Unimod unimod = unimodDAO.getUnimod(unimodHashMap.get(i));
                if (unimod != null) {
                    monoWeight += unimod.getMonoMass();
                }
            }
            monoWeight += aa.getMonoIsotopicMass();
//            if (i == 0) {
//                continue;
//            }
            bSeries.add(monoWeight);
        }

        //ySeries
        List<Double> ySeries = new ArrayList<>();
        monoWeight = Constants.PROTON_MASS_U * charge;
//        if (unimodHashMap != null && unimodHashMap.containsKey(acidCodeArray.length - 1)) {
//            Unimod unimod = unimodDAO.getUnimod(unimodHashMap.get(acidCodeArray.length - 1));
//            if (unimod != null) {
//                monoWeight += unimod.getMonoMass();
//            }
//        }

        double h2oWeight = elementsDAO.getMonoWeight(ElementsDAO.H2O);
        for (int i = acidCodeArray.length - 1; i > 0; i--) {
            AminoAcid aa = aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCodeArray[i]));
            if (aa == null) {
                continue;
            }
            if (unimodHashMap != null && unimodHashMap.containsKey(i)) {
                Unimod unimod = unimodDAO.getUnimod(unimodHashMap.get(i));
                if (unimod != null) {
                    monoWeight += unimod.getMonoMass();
                }
            }
            monoWeight += aa.getMonoIsotopicMass();
            ySeries.add(monoWeight + h2oWeight);
        }

        bySeries.setBSeries(bSeries);
        bySeries.setYSeries(ySeries);

        return bySeries;
    }

    public BYSeries getBYSeriesOld(HashMap<Integer, String> unimodHashMap, String sequence, int charge) {

        BYSeries bySeries = new BYSeries();

        //bSeries 若要提高精度，提高json的精度
        List<Double> bSeries = new ArrayList<>();
        double monoWeight = Constants.PROTON_MASS_U * charge;
        if (unimodHashMap != null && unimodHashMap.containsKey(0)) {
            Unimod unimod = unimodDAO.getUnimod(unimodHashMap.get(0));
            if (unimod != null) {
                monoWeight += unimod.getMonoMass();
            }
        }

        char[] acidCodeArray = sequence.toCharArray();
        for (int i = 0; i < acidCodeArray.length - 1; i++) {
            AminoAcid aa = aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCodeArray[i]));
            if (aa == null) {
                continue;
            }
            if (i == 0) {
                monoWeight += aa.getMonoIsotopicMass();
                continue;
            }
            monoWeight += aa.getMonoIsotopicMass();
            bSeries.add(monoWeight);
        }

        //ySeries
        List<Double> ySeries = new ArrayList<>();
        monoWeight = Constants.PROTON_MASS_U * charge;
        if (unimodHashMap != null && unimodHashMap.containsKey(acidCodeArray.length - 1)) {
            Unimod unimod = unimodDAO.getUnimod(unimodHashMap.get(acidCodeArray.length - 1));
            if (unimod != null) {
                monoWeight += unimod.getMonoMass();
            }
        }

        double h2oWeight = elementsDAO.getMonoWeight(ElementsDAO.H2O);
        for (int i = acidCodeArray.length - 1; i > 0; i--) {
            AminoAcid aa = aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCodeArray[i]));
            if (aa == null) {
                continue;
            }
            monoWeight += aa.getMonoIsotopicMass();
            ySeries.add(monoWeight + h2oWeight);
        }

        bySeries.setBSeries(bSeries);
        bySeries.setYSeries(ySeries);

        return bySeries;
    }

    /**
     * 标准库中的PeptideDO对象生成该肽段所有B,Y类型的排列组合的离子MZ的Map,key为cutInfo
     *
     * @param peptideDO   标准库中的PeptideDO对象
     * @param limitLength 生成的B,Y离子的最小长度
     * @return
     */
    public HashMap<String, Double> getBYSeriesMap(PeptideDO peptideDO, int limitLength) {
        HashMap<String, Double> bySeriesMap = new HashMap<>();
        String sequence = peptideDO.getSequence();
        int length = sequence.length();
        if (length < limitLength) {
            return null;
        }
        for (int c = 1; c <= peptideDO.getCharge(); c++) {
            for (int i = limitLength; i < length; i++) {
                String bSubstring = sequence.substring(0, i);
                String ySubstring = sequence.substring(length - i, length);
                List<String> bUnimodIds = formulaCalculator.parseUnimodIds(peptideDO.getUnimodMap(), 0, i);
                List<String> yUnimodIds = formulaCalculator.parseUnimodIds(peptideDO.getUnimodMap(), length - i, length);

                Double bMz = formulaCalculator.getMonoMz(bSubstring, ResidueType.BIon, c, 0, 0, false, bUnimodIds);
                bySeriesMap.put("b" + i + (c == 1 ? "" : ("^" + c)), bMz);
                Double yMz = formulaCalculator.getMonoMz(ySubstring, ResidueType.YIon, c, 0, 0, false, yUnimodIds);
                bySeriesMap.put("y" + i + (c == 1 ? "" : ("^" + c)), yMz);
            }
        }

        return bySeriesMap;
    }

    public double getTheoryMass(HashMap<Integer, String> unimodHashMap, String sequence){
        double totalMass = Constants.B_SIDE_MASS + Y_SIDE_MASS;
        char[] acidCodeArray = sequence.toCharArray();
        for (char acidCode: acidCodeArray) {
            AminoAcid aa = aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCode));
            totalMass += aa.getMonoIsotopicMass();
        }
        for (String unimodCode: unimodHashMap.values()){
            Unimod unimod = unimodDAO.getUnimod(unimodCode);
            if (unimod != null) {
                totalMass += unimod.getMonoMass();
            }
        }
        return totalMass;
    }

    public Fragment getFragment(PeptideDO peptide, FragmentInfo fragmentInfo) {
        Fragment fragment = new Fragment(peptide.getId());
        String sequence = peptide.getSequence();
        Annotation annotation = fragmentInfo.getAnnotation();
        fragment.setUnimodMap(peptide.getUnimodMap());

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

        return fragment;
    }

    /**
     * 获取不包含计算分子质量数据的fragment,也不包含修饰基团
     *
     * @param peptideDO
     * @return
     */
    public Fragment getBaseFragment(PeptideDO peptideDO, FragmentInfo fragmentInfo) {
        Fragment fragment = new Fragment();
        String sequence = peptideDO.getSequence();
        Annotation annotation = fragmentInfo.getAnnotation();
        fragment.setUnimodMap(peptideDO.getUnimodMap());

        String fs = getFragmentSequence(sequence, annotation.getType(), annotation.getLocation());

        fragment.setIsotope(annotation.isIsotope());
        fragment.setDeviation(annotation.getDeviation());
        fragment.setLocation(annotation.getLocation());
        fragment.setAdjust(annotation.getAdjust());
        fragment.setSequence(fs);
        fragment.setType(annotation.getType());
        fragment.setCharge(annotation.getCharge());

        return fragment;
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

        PeptideQuery query = new PeptideQuery();
        query.setLibraryId(libraryId);
        query.setPageSize(MAX_PAGE_SIZE_FOR_FRAGMENT);
        long totalCount = peptideService.count(query);
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
            ResultDO<List<PeptideDO>> resultTmp = peptideService.getList(query);
            logger.info("读取第" + i + "批数据,总计" + totalPage + "批");
            List<PeptideDO> list = resultTmp.getModel();

            for (PeptideDO peptide : list) {
                for (FragmentInfo fragmentInfo : peptide.getFragmentMap().values()) {
                    Fragment fragment = getBaseFragment(peptide, fragmentInfo);
                    if (fragment != null) {
                        if (peptide.getIsDecoy()) {
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

    public List<AminoAcid> parseAminoAcid(String sequence, HashMap<Integer, String> unimodMap) {

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
        PeptideQuery query = new PeptideQuery();
        query.setLibraryId(libraryId);
        query.setIsDecoy(isDecoy);
        query.setPageSize(MAX_PAGE_SIZE_FOR_FRAGMENT);
        long totalCount = peptideService.count(query);
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
            ResultDO<List<PeptideDO>> resultDO = peptideService.getList(query);
            logger.info("读取第" + i + "批数据,总计" + totalPage + "批");
            List<PeptideDO> list = resultDO.getModel();

            for (PeptideDO peptide : list) {
                for (FragmentInfo fragmentInfo : peptide.getFragmentMap().values()) {
                    Fragment fragment = getFragment(peptide, fragmentInfo);
                    if (fragment != null) {
                        count++;
                        if (!fragments.contains(fragment)) {
                            count++;
                            fragments.add(fragment);
                            double result = Math.abs(fragmentInfo.getMz() - fragment.getMonoMz());
                            if (result > threshold) {
                                MzResult mzResult = new MzResult();
                                mzResult.setOriginMz(fragmentInfo.getMz());
                                mzResult.setNewMz(fragment.getMonoMz());
                                mzResult.setDelta(result);
                                mzResult.setCharge(fragment.getCharge());
                                mzResult.setSequence(peptide.getSequence());
                                mzResult.setType(fragment.getType());
                                mzResult.setOriginSequence(peptide.getFullName());
                                mzResult.setFragmentSequence(fragment.getSequence());
                                mzResult.setAnnotations(fragmentInfo.getAnnotations());
                                mzResult.setPrecursorCharge(peptide.getCharge());
                                mzResult.setPrecursorMz(peptide.getMz());
                                mzResult.setNewPrecursorMz(formulaCalculator.getMonoMz(peptide));
                                mzResult.setLocation(fragment.getLocation());
                                if (Math.abs(peptide.getMz() - mzResult.getNewPrecursorMz()) > threshold) {
                                    mzResult.setDelatPrecursorMz(Math.abs(peptide.getMz() - mzResult.getNewPrecursorMz()));
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
