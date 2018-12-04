//package com.westlake.air.pecs.test.algorithm;
//
//import com.westlake.air.pecs.algorithm.FormulaCalculator;
//import com.westlake.air.pecs.constants.ResidueType;
//import com.westlake.air.pecs.domain.db.FragmentInfo;
//import com.westlake.air.pecs.domain.db.PeptideDO;
//import com.westlake.air.pecs.test.BaseTest;
//import org.junit.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
///**
// * Created by James Lu MiaoShan
// * Time: 2018-08-08 19:00
// */
//
//public class FormulaCalculatorTest extends BaseTest {
//
//    @Autowired
//    FormulaCalculator formulaCalculator;
//
//    public double range = 0.0000001;
//
//    @Test
//    public void getMonoMzTest_RealSample_TransitionDO_1() {
//        double result = formulaCalculator.getMonoMz(getJsonFromFileTest());
//        double expect = (71.037114 + 57.021464 + 99.068414 + 129.042593 + 101.047679 * 3 + 97.052764 + 87.032028 + 128.094963 + 1.0078250319 * 4 + 15.994915) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample_TransitionDO_2() {
//        PeptideDO peptideDO = getJsonFromFileTest();
//        peptideDO.setPrecursorCharge(1);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        double expect = (71.037114 + 57.021464 + 99.068414 + 129.042593 + 101.047679 * 3 + 97.052764 + 87.032028 + 128.094963 + 1.0078250319 * 3 + 15.994915);
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample_TransitionDO_3() {
//        PeptideDO peptideDO = getJsonFromFileTest();
//        peptideDO.setPrecursorCharge(0);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        assert (Double.isNaN(result));
//    }
//
//
//    @Test
//    public void getMonoMzTest_RealSample_TransitionDO_4() {
//        PeptideDO peptideDO = null;
//        Double result = formulaCalculator.getMonoMz(peptideDO);
//        System.out.print(result);
//        assert (result == 0);
//    }
//
//    //该测试仅保留了Sequence变量以及annotation,其他变量置0
//    @Test
//    public void getMonoMzTest_RealSample_TransitionDO_5() {
//        PeptideDO peptideDO = getJsonFromFileTest();
//        FragmentInfo fi = new FragmentInfo();
//        peptideDO.setId(null);
//        peptideDO.setLibraryId(null);
//        peptideDO.setPeptideRef(null);
//        peptideDO.setLibraryName(null);
//        peptideDO.setMz(null);
//        fi.setMz(null);
//        peptideDO.setRt(null);
//        fi.setIntensity(null);
//        peptideDO.setIsDecoy(true);
//        fi.setAnnotations(null);
//        fi.setWithBrackets(true);
//        peptideDO.setFullName(null);
//        fi.setAnnotations(null);
//        fi.setCutInfo(null);
//        peptideDO.setAcidList(null);
//        peptideDO.putFragment(fi.getCutInfo(), fi);
//        Double result = formulaCalculator.getMonoMz(peptideDO);
//        double expect = (71.037114 + 57.021464 + 99.068414 + 129.042593 + 101.047679 * 3 + 97.052764 + 87.032028 + 128.094963 + 1.0078250319 * 4 + 15.994915) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample_TransitionDO_6() {
//        PeptideDO peptideDO = getJsonFromFileTest();
//        FragmentInfo fi = new FragmentInfo();
//        peptideDO.setId(null);
//        peptideDO.setLibraryId(null);
//        peptideDO.setPeptideRef(null);
//        peptideDO.setLibraryName(null);
//        peptideDO.setMz(null);
//        fi.setMz(null);
//        peptideDO.setRt(null);
//        fi.setIntensity(null);
//        peptideDO.setIsDecoy(true);
//        peptideDO.setSequence("AGVETTTPSKVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV");
//        fi.setAnnotations(null);
//        fi.setWithBrackets(true);
//        peptideDO.setFullName(null);
//        peptideDO.setCharge(1);
//        fi.setAnnotations(null);
//        fi.setCutInfo(null);
//        peptideDO.setAcidList(null);
//        peptideDO.putFragment(fi.getCutInfo(), fi);
//        Double result = formulaCalculator.getMonoMz(peptideDO);
//        assert (Math.abs(result - 5943) < 1);
//    }
//
//    //Sequence中包含不存在氨基酸对应的字符时没有返回值
//    @Test
//    public void getMonoMzTest_RealSample_TransitionDO_7() {
//        PeptideDO peptideDO = getJsonFromFileTest();
//        peptideDO.setId(null);
//        peptideDO.setLibraryId(null);
//        peptideDO.setPeptideRef(null);
//        peptideDO.setLibraryName(null);
//        peptideDO.setMz(null);
//        peptideDO.setProductMz(null);
//        peptideDO.setRt(null);
//        peptideDO.setName(null);
//        peptideDO.setIntensity(null);
//        peptideDO.setIsDecoy(true);
//        peptideDO.setSequence("AGVETTTPSKJ");
//        peptideDO.setAnnotations(null);
//        peptideDO.setWithBrackets(true);
//        peptideDO.setFullName(null);
//        peptideDO.setPrecursorCharge(2);
//        peptideDO.setAnnotations(null);
//        peptideDO.setCutInfo(null);
//        peptideDO.setAcidList(null);
//        peptideDO.setDetecting(false);
//        peptideDO.setIdentifying(true);
//        peptideDO.setQuantifying(false);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        double expect = (71.037114 + 57.021464 + 99.068414 + 129.042593 + 101.047679 * 3 + 97.052764 + 87.032028 + 128.094963 + 1.0078250319 * 4 + 15.994915) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample_TransitionDO_8() {
//        PeptideDO peptideDO = getJsonFromFileTest();
//        peptideDO.setId(null);
//        peptideDO.setLibraryId(null);
//        peptideDO.setPeptideRef(null);
//        peptideDO.setLibraryName(null);
//        peptideDO.setMz(null);
//        peptideDO.setProductMz(null);
//        peptideDO.setRt(null);
//        peptideDO.setName(null);
//        peptideDO.setIntensity(null);
//        peptideDO.setIsDecoy(true);
//        peptideDO.setSequence(" AGVETTTPSK" + " ");
//        peptideDO.setAnnotations(null);
//        peptideDO.setWithBrackets(true);
//        peptideDO.setFullName(null);
//        peptideDO.setPrecursorCharge(2);
//        peptideDO.setAnnotations(null);
//        peptideDO.setCutInfo(null);
//        peptideDO.setAcidList(null);
//        peptideDO.setDetecting(false);
//        peptideDO.setIdentifying(true);
//        peptideDO.setQuantifying(false);
//        Double result = formulaCalculator.getMonoMz(peptideDO);
//        double expect = (71.037114 + 57.021464 + 99.068414 + 129.042593 + 101.047679 * 3 + 97.052764 + 87.032028 + 128.094963 + 1.0078250319 * 4 + 15.994915) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample1_TransitionDO_1() {
//        double result = formulaCalculator.getMonoMz(getJsonFromFileTest1());
//        Double expect = (113.084064 * 2 + 163.06332 * 2 + 129.042593 + 71.037114 + 156.101111 + 1.0078250319 * 2 + 15.994915 + 1.0078250319 * 2) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample1_TransitionDO_2() {
//        PeptideDO peptideDO = getJsonFromFileTest1();
//        peptideDO.setPrecursorCharge(1);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        Double expect = 113.084064 * 2 + 163.06332 * 2 + 129.042593 + 71.037114 + 156.101111 + 1.0078250319 * 2 + 15.994915 + 1.0078250319;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample1_TransitionDO_3() {
//        PeptideDO peptideDO = getJsonFromFileTest1();
//        peptideDO.setId(null);
//        peptideDO.setLibraryId(null);
//        peptideDO.setPeptideRef(null);
//        peptideDO.setLibraryName(null);
//        peptideDO.setMz(null);
//        peptideDO.setProductMz(null);
//        peptideDO.setRt(null);
//        peptideDO.setName(null);
//        peptideDO.setIntensity(null);
//        peptideDO.setIsDecoy(true);
//        peptideDO.setAnnotations(null);
//        peptideDO.setWithBrackets(true);
//        peptideDO.setFullName(null);
//        peptideDO.setAnnotations(null);
//        peptideDO.setCutInfo(null);
//        peptideDO.setAcidList(null);
//        peptideDO.setDetecting(false);
//        peptideDO.setIdentifying(true);
//        peptideDO.setQuantifying(false);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        Double expect = (113.084064 * 2 + 163.06332 * 2 + 129.042593 + 71.037114 + 156.101111 + 1.0078250319 * 2 + 15.994915 + 1.0078250319 * 2) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    //检测到result为Infinity,后续是否有影响需要再判断
//    @Test
//    public void getMonoMzTest_RealSample1_TransitionDO_4() {
//        PeptideDO peptideDO = getJsonFromFileTest1();
//        peptideDO.setPrecursorCharge(0);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        assert Double.isNaN(result);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample2_TransitionDO_1() {
//        double result = formulaCalculator.getMonoMz(getJsonFromFileTest2());
//        Double expect = (128.094963 + 99.068414 + 97.052764 + 128.058578 + 99.068414 + 87.032028 + 101.047679 + 97.052764 + 101.047679 + 113.084064 + 99.068414 + 129.042593 + 99.068414 + 87.032028 + 156.101111 + 1.0078250319 * 5 + 15.994915) / 3;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample2_TransitionDO_2() {
//        PeptideDO peptideDO = getJsonFromFileTest2();
//        peptideDO.setPrecursorCharge(1);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        Double expect = (128.094963 + 99.068414 + 97.052764 + 128.058578 + 99.068414 + 87.032028 + 101.047679 + 97.052764 + 101.047679 + 113.084064 + 99.068414 + 129.042593 + 99.068414 + 87.032028 + 156.101111 + 1.0078250319 * 3 + 15.994915);
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample2_TransitionDO_3() {
//        PeptideDO peptideDO = getJsonFromFileTest2();
//        peptideDO.setId(null);
//        peptideDO.setLibraryId(null);
//        peptideDO.setPeptideRef(null);
//        peptideDO.setLibraryName(null);
//        peptideDO.setMz(null);
//        peptideDO.setProductMz(null);
//        peptideDO.setRt(null);
//        peptideDO.setName(null);
//        peptideDO.setIntensity(null);
//        peptideDO.setIsDecoy(true);
//        peptideDO.setAnnotations(null);
//        peptideDO.setWithBrackets(true);
//        peptideDO.setFullName(null);
//        peptideDO.setAnnotations(null);
//        peptideDO.setCutInfo(null);
//        peptideDO.setAcidList(null);
//        peptideDO.setDetecting(false);
//        peptideDO.setIdentifying(true);
//        peptideDO.setQuantifying(false);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        Double expect = (128.094963 + 99.068414 + 97.052764 + 128.058578 + 99.068414 + 87.032028 + 101.047679 + 97.052764 + 101.047679 + 113.084064 + 99.068414 + 129.042593 + 99.068414 + 87.032028 + 156.101111 + 1.0078250319 * 5 + 15.994915) / 3;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    //检测到result为Infinity,后续是否有影响需要再判断
//    @Test
//    public void getMonoMzTest_RealSample2_TransitionDO_4() {
//        PeptideDO peptideDO = getJsonFromFileTest2();
//        peptideDO.setPrecursorCharge(0);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        assert Double.isNaN(result);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample3_TransitionDO_1() {
//        double result = formulaCalculator.getMonoMz(getJsonFromFileTest3());
//        double expect = (99.068414 + 137.058912 + 101.047679 + 129.042593 + 103.009185 * 2 + 137.058912 + 57.021464 + 115.026943 + 113.084064 * 2 + 129.042593 + 103.009185 + 71.037114 + 115.026943 * 2 + 156.101111 + 1.0078250319 * 6 + 15.994915 + 57.021464 * 3) / 4;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample3_TransitionDO_2() {
//        PeptideDO peptideDO = getJsonFromFileTest3();
//        peptideDO.setPrecursorCharge(1);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        Double expect = (99.068414 + 137.058912 + 101.047679 + 129.042593 + 103.009185 * 2 + 137.058912 + 57.021464 + 115.026943 + 113.084064 * 2 + 129.042593 + 103.009185 + 71.037114 + 115.026943 * 2 + 156.101111 + 1.0078250319 * 3 + 15.994915 + 57.021464 * 3);
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample3_TransitionDO_3() {
//        PeptideDO peptideDO = getJsonFromFileTest3();
//        peptideDO.setId(null);
//        peptideDO.setLibraryId(null);
//        peptideDO.setPeptideRef(null);
//        peptideDO.setLibraryName(null);
//        peptideDO.setMz(null);
//        peptideDO.setProductMz(null);
//        peptideDO.setRt(null);
//        peptideDO.setName(null);
//        peptideDO.setIntensity(null);
//        peptideDO.setIsDecoy(true);
//        peptideDO.setAnnotations(null);
//        peptideDO.setWithBrackets(true);
//        peptideDO.setFullName(null);
//        peptideDO.setAnnotations(null);
//        peptideDO.setCutInfo(null);
//        peptideDO.setAcidList(null);
//        peptideDO.setDetecting(false);
//        peptideDO.setIdentifying(true);
//        peptideDO.setQuantifying(false);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        Double expect = (99.068414 + 137.058912 + 101.047679 + 129.042593 + 103.009185 * 2 + 137.058912 + 57.021464 + 115.026943 + 113.084064 * 2 + 129.042593 + 103.009185 + 71.037114 + 115.026943 * 2 + 156.101111 + 1.0078250319 * 6 + 15.994915 + 57.021464 * 3) / 4;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    //检测到result为Infinity,后续是否有影响需要再判断
//    @Test
//    public void getMonoMzTest_RealSample3_TransitionDO_4() {
//        PeptideDO peptideDO = getJsonFromFileTest3();
//        peptideDO.setPrecursorCharge(0);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        assert Double.isNaN(result);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample4_TransitionDO_1() {
//        double result = formulaCalculator.getMonoMz(getJsonFromFileTest4());
//        double expect = (101.047679 + 103.009185 + 99.068414 + 71.037114 + 115.026943 + 129.042593 + 87.032028 + 71.037114 + 129.042593 + 114.042927 + 103.009185 + 115.026943 + 128.094963 + 1.0078250319 * 4 + 15.994915 + 57.021464 * 2) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample4_TransitionDO_2() {
//        PeptideDO peptideDO = getJsonFromFileTest4();
//        peptideDO.setPrecursorCharge(1);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        double expect = (101.047679 + 103.009185 + 99.068414 + 71.037114 + 115.026943 + 129.042593 + 87.032028 + 71.037114 + 129.042593 + 114.042927 + 103.009185 + 115.026943 + 128.094963 + 1.0078250319 * 3 + 15.994915 + 57.021464 * 2);
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample4_TransitionDO_3() {
//        PeptideDO peptideDO = getJsonFromFileTest4();
//        peptideDO.setId(null);
//        peptideDO.setLibraryId(null);
//        peptideDO.setPeptideRef(null);
//        peptideDO.setLibraryName(null);
//        peptideDO.setMz(null);
//        peptideDO.setProductMz(null);
//        peptideDO.setRt(null);
//        peptideDO.setName(null);
//        peptideDO.setIntensity(null);
//        peptideDO.setIsDecoy(true);
//        peptideDO.setAnnotations(null);
//        peptideDO.setWithBrackets(true);
//        peptideDO.setFullName(null);
//        peptideDO.setAnnotations(null);
//        peptideDO.setCutInfo(null);
//        peptideDO.setAcidList(null);
//        peptideDO.setDetecting(false);
//        peptideDO.setIdentifying(true);
//        peptideDO.setQuantifying(false);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        double expect = (101.047679 + 103.009185 + 99.068414 + 71.037114 + 115.026943 + 129.042593 + 87.032028 + 71.037114 + 129.042593 + 114.042927 + 103.009185 + 115.026943 + 128.094963 + 1.0078250319 * 4 + 15.994915 + 57.021464 * 2) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest();
//        double result = formulaCalculator.getMonoMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        double expect = (71.037114 + 57.021464 + 99.068414 + 129.042593 + 101.047679 * 3 + 97.052764 + 87.032028 + 128.094963 + 1.0078250319 * 4 + 15.994915) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample1_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest1();
//        double result = formulaCalculator.getMonoMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        Double expect = (113.084064 * 2 + 163.06332 * 2 + 129.042593 + 71.037114 + 156.101111 + 1.0078250319 * 2 + 15.994915 + 1.0078250319 * 2) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample2_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest2();
//        double result = formulaCalculator.getMonoMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        Double expect = (128.094963 + 99.068414 + 97.052764 + 128.058578 + 99.068414 + 87.032028 + 101.047679 + 97.052764 + 101.047679 + 113.084064 + 99.068414 + 129.042593 + 99.068414 + 87.032028 + 156.101111 + 1.0078250319 * 5 + 15.994915) / 3;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample3_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest3();
//        double result = formulaCalculator.getMonoMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        double expect = (99.068414 + 137.058912 + 101.047679 + 129.042593 + 103.009185 * 2 + 137.058912 + 57.021464 + 115.026943 + 113.084064 * 2 + 129.042593 + 103.009185 + 71.037114 + 115.026943 * 2 + 156.101111 + 1.0078250319 * 6 + 15.994915) / 4;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getMonoMzTest_RealSample4_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest4();
//        double result = formulaCalculator.getMonoMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        double expect = (101.047679 + 103.009185 + 99.068414 + 71.037114 + 115.026943 + 129.042593 + 87.032028 + 71.037114 + 129.042593 + 114.042927 + 103.009185 + 115.026943 + 128.094963 + 1.0078250319 * 4 + 15.994915) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    //检测到result为Infinity,后续是否有影响需要再判断
//    @Test
//    public void getMonoMzTest_RealSample4_TransitionDO_4() {
//        PeptideDO peptideDO = getJsonFromFileTest4();
//        peptideDO.setPrecursorCharge(0);
//        double result = formulaCalculator.getMonoMz(peptideDO);
//        assert Double.isNaN(result);
//    }
//
//    @Test
//    public void getAverageMz_RealSample_TransitionDO() {
//        double result = formulaCalculator.getAverageMz(getJsonFromFileTest());
//        double expect = (71.0779 + 57.0513 + 99.1311 + 129.114 + 101.1039 * 3 + 97.1152 + 87.0773 + 128.1723 + 1.0079407537168315 * 4 + 15.999405323160001) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample1_TransitionDO() {
//        double result = formulaCalculator.getAverageMz(getJsonFromFileTest1());
//        double expect = (163.1733 * 2 + 113.1576 * 2 + 129.114 + 71.0779 + 156.1857 + 1.0079407537168315 * 4 + 15.999405323160001) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample2_TransitionDO() {
//        double result = formulaCalculator.getAverageMz(getJsonFromFileTest2());
//        double expect = (128.1723 + 99.1311 + 97.1152 + 128.1292 + 99.1311 + 87.0773 + 101.1039 + 97.1152 + 101.1039 + 113.1576 + 99.1311 + 129.114 + 99.1311 + 87.0773 + 156.1857 + 1.0079407537168315 * 5 + 15.999405323160001) / 3;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample3_TransitionDO() {
//        double result = formulaCalculator.getAverageMz(getJsonFromFileTest3());
//        double expect = (99.1311 + 137.1393 + 101.1039 + 129.114 + 103.1429 * 2 + 137.1393 + 57.0513 + 115.0874 + 113.1576 * 2 + 129.114 + 103.1429 + 71.0779 + 115.0874 * 2 + 156.1857 + 1.0079407537168315 * 6 + 15.999405323160001 + 57.0513 * 3) / 4;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample4_TransitionDO() {
//        double result = formulaCalculator.getAverageMz(getJsonFromFileTest4());
//        double expect = (101.1039 + 103.1429 + 99.1311 + 71.0779 + 115.0874 + 129.114 + 87.0773 + 71.0779 + 129.114 + 114.1026 + 103.1429 + 115.0874 + 128.1723 + 1.0079407537168315 * 4 + 15.999405323160001 + 57.0513 * 2) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest();
//        double result = formulaCalculator.getAverageMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        double expect = (71.0779 + 57.0513 + 99.1311 + 129.114 + 101.1039 * 3 + 97.1152 + 87.0773 + 128.1723 + 1.0079407537168315 * 4 + 15.999405323160001) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample1_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest1();
//        double result = formulaCalculator.getAverageMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        double expect = (163.1733 * 2 + 113.1576 * 2 + 129.114 + 71.0779 + 156.1857 + 1.0079407537168315 * 4 + 15.999405323160001) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample2_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest2();
//        double result = formulaCalculator.getAverageMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        double expect = (128.1723 + 99.1311 + 97.1152 + 128.1292 + 99.1311 + 87.0773 + 101.1039 + 97.1152 + 101.1039 + 113.1576 + 99.1311 + 129.114 + 99.1311 + 87.0773 + 156.1857 + 1.0079407537168315 * 5 + 15.999405323160001) / 3;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample3_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest3();
//        double result = formulaCalculator.getAverageMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        double expect = (99.1311 + 137.1393 + 101.1039 + 129.114 + 103.1429 * 2 + 137.1393 + 57.0513 + 115.0874 + 113.1576 * 2 + 129.114 + 103.1429 + 71.0779 + 115.0874 * 2 + 156.1857 + 1.0079407537168315 * 6 + 15.999405323160001) / 4;
//        assert (Math.abs(result - expect) < range);
//    }
//
//    @Test
//    public void getAverageMz_RealSample4_Separate() {
//        PeptideDO peptideDO = getJsonFromFileTest4();
//        double result = formulaCalculator.getAverageMz(peptideDO.getSequence(), ResidueType.Full, peptideDO.getPrecursorCharge(), 0, 0, false, null);
//        double expect = (101.1039 + 103.1429 + 99.1311 + 71.0779 + 115.0874 + 129.114 + 87.0773 + 71.0779 + 129.114 + 114.1026 + 103.1429 + 115.0874 + 128.1723 + 1.0079407537168315 * 4 + 15.999405323160001) / 2;
//        assert (Math.abs(result - expect) < range);
//    }
//}
//
//
//
//
