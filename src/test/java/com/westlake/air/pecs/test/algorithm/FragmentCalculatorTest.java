package com.westlake.air.pecs.test.algorithm;

import com.westlake.air.pecs.algorithm.FormulaCalculator;
import com.westlake.air.pecs.algorithm.FragmentCalculator;
import com.westlake.air.pecs.domain.bean.analyse.MzResult;
import com.westlake.air.pecs.domain.bean.transition.AminoAcid;
import com.westlake.air.pecs.domain.bean.transition.Fragment;
import com.westlake.air.pecs.domain.bean.transition.FragmentResult;
import com.westlake.air.pecs.domain.db.FragmentInfo;
import com.westlake.air.pecs.domain.db.PeptideDO;
import com.westlake.air.pecs.service.PeptideService;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: An Shaowei
 * @Time: 2018/8/9 15:44
 */
public class FragmentCalculatorTest extends BaseTest {
    @Autowired
    FragmentCalculator fragmentCalculator;
    @Autowired
    PeptideService peptideService;
    @Autowired
    FormulaCalculator formulaCalculator;

    @Test
    public void getFragmentsTest() {
        PeptideDO peptideDO = getJsonFromFileTest();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setSequence(fragmentCalculator.getFragmentSequence("AGVETTTPSK","y",7));
        fragment.setStart(3);
        fragment.setEnd(9);
        fragment.setIsotope(false);
        fragment.setDeviation(0.0);
        fragment.setLocation(7);
        fragment.setAdjust(0);
        fragment.setType("y");
        fragment.setCharge(1);
        fragment.setMonoMz(formulaCalculator.getMonoMz(fragment));
        fragment.setAverageMz(formulaCalculator.getAverageMz(fragment));
        assert (result.equals(fragment));
    }

    @Test
    public void getFragmentsTest_1() {
        PeptideDO peptideDO = getJsonFromFileTest1();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setSequence(fragmentCalculator.getFragmentSequence("YLYEIAR","b",3));
        fragment.setStart(0);
        fragment.setEnd(6);
        fragment.setIsotope(false);
        fragment.setDeviation(-0.0);
        fragment.setLocation(3);
        fragment.setAdjust(0);
        fragment.setType("b");
        fragment.setCharge(1);
        fragment.setMonoMz(formulaCalculator.getMonoMz(fragment));
        fragment.setAverageMz(formulaCalculator.getAverageMz(fragment));
        assert (result.equals(fragment));
    }

    @Test
    public void getFragmentsTest_2() {
        PeptideDO peptideDO = getJsonFromFileTest2();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setSequence(fragmentCalculator.getFragmentSequence("KVPQVSTPTLVEVSR","y",5));
        fragment.setStart(10);
        fragment.setEnd(14);
        fragment.setIsotope(false);
        fragment.setDeviation(0.0);
        fragment.setLocation(5);
        fragment.setAdjust(0);
        fragment.setType("y");
        fragment.setCharge(1);
        fragment.setMonoMz(formulaCalculator.getMonoMz(fragment));
        fragment.setAverageMz(formulaCalculator.getAverageMz(fragment));
        assert (result.equals(fragment));
    }

    @Test
    public void getFragmentsTest_3() {
        PeptideDO peptideDO = getJsonFromFileTest3();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setSequence(fragmentCalculator.getFragmentSequence("VHTECCHGDLLECADDR","b",10));
        fragment.setStart(0);
        fragment.setEnd(16);
        fragment.setIsotope(false);
        fragment.setDeviation(0.0);
        fragment.setLocation(10);
        fragment.setAdjust(0);
        fragment.setType("b");
        fragment.setCharge(2);
        fragment.setMonoMz(formulaCalculator.getMonoMz(fragment));
        fragment.setAverageMz(formulaCalculator.getAverageMz(fragment));
        fragment.setUnimodMap(peptideDO.getUnimodMap());
        assert (result.equals(fragment));
    }

    @Test
    public void getBaseFragmentsTest() {
        PeptideDO peptideDO = getJsonFromFileTest();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setIsotope(false);
        fragment.setDeviation(0.0);
        fragment.setLocation(7);
        fragment.setAdjust(0);
        fragment.setSequence(fragmentCalculator.getFragmentSequence("AGVETTTPSK", "y", 7));
        fragment.setType("y");
        fragment.setCharge(1);
        assert (result.equals(fragment));
    }

    @Test
    public void getBaseFragmentsTest_1() {
        PeptideDO peptideDO = getJsonFromFileTest1();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setIsotope(false);
        fragment.setDeviation(-0.0);
        fragment.setLocation(3);
        fragment.setAdjust(0);
        fragment.setSequence(fragmentCalculator.getFragmentSequence("YLYEIAR", "b", 3));
        fragment.setType("b");
        fragment.setCharge(1);
        assert (result.equals(fragment));
    }

    @Test
    public void getBaseFragmentsTest_2() {
        PeptideDO peptideDO = getJsonFromFileTest2();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setIsotope(false);
        fragment.setDeviation(-0.0);
        fragment.setLocation(5);
        fragment.setAdjust(0);
        fragment.setSequence(fragmentCalculator.getFragmentSequence("KVPQVSTPTLVEVSR", "y", 5));
        fragment.setType("y");
        fragment.setCharge(1);
        assert (result.equals(fragment));
    }

    @Test
    public void getBaseFragmentsTest_3() {
        PeptideDO peptideDO = getJsonFromFileTest3();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setIsotope(false);
        fragment.setDeviation(0.0);
        fragment.setLocation(10);
        fragment.setAdjust(0);
        fragment.setSequence(fragmentCalculator.getFragmentSequence("VHTECCHGDLLECADDR", "b", 10));
        fragment.setType("b");
        fragment.setCharge(2);
        assert (result.equals(fragment));
    }

    @Test
    public void getBaseFragmentsTest_4() {
        PeptideDO peptideDO = getJsonFromFileTest4();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        Fragment result = fragmentCalculator.getFragment(peptideDO, fi);
        Fragment fragment = new Fragment();
        fragment.setIsotope(false);
        fragment.setDeviation(-0.0);
        fragment.setLocation(11);
        fragment.setAdjust(0);
        fragment.setSequence(fragmentCalculator.getFragmentSequence("TCVADESAENCDK", "y", 11));
        fragment.setType("y");
        fragment.setCharge(1);
        assert (result.equals(fragment));
    }


    @Test
    public void getFragmentSequenceTest_1() {
        PeptideDO peptideDO = getJsonFromFileTest();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        String result = fragmentCalculator.getFragmentSequence(peptideDO.getSequence(), fi.getAnnotation().getType(), fi.getAnnotation().getLocation());
        String expect = "AGVETTTPSK".substring(3, 10);
        assert (result.equals(expect));
    }

    @Test
    public void getFragmentSequenceTest_2() {
        PeptideDO peptideDO = getJsonFromFileTest3();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        String result = fragmentCalculator.getFragmentSequence(peptideDO.getSequence(), fi.getAnnotation().getType(), fi.getAnnotation().getLocation());
        String expect = "VHTECCHGDLLECADDR".substring(0, 10);
        assert (result.equals(expect));
    }

    @Test
    public void getFragmentSequenceTest_3() {
        PeptideDO peptideDO = getJsonFromFileTest2();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        String result = fragmentCalculator.getFragmentSequence(peptideDO.getSequence(), fi.getAnnotation().getType(), fi.getAnnotation().getLocation());
        String expect = "KVPQVSTPTLVEVSR".substring(10, 15);
        assert (result.equals(expect));
    }

    @Test
    public void getFragmentSequenceTest_4() {
        PeptideDO peptideDO = getJsonFromFileTest4();
        FragmentInfo fi = peptideDO.getFragmentMap().values().iterator().next();
        String result = fragmentCalculator.getFragmentSequence(peptideDO.getSequence(), fi.getAnnotation().getType(), fi.getAnnotation().getLocation());
        String expect = "TCVADESAENCDK".substring(2, 13);
        assert (result.equals(expect));
    }

    @Test
    public void decoyOverviewTest() {
        PeptideDO transition = getJsonFromFileTest();
        FragmentResult result = fragmentCalculator.decoyOverview("5b6712012ada5f2dc8de57d7");
        FragmentResult fragmentResult = new FragmentResult();

    }

    @Test
    public void parseAminoAcidTest() {
        PeptideDO peptideDO = getJsonFromFileTest();
        List<AminoAcid> result = fragmentCalculator.parseAminoAcid(peptideDO.getSequence(), peptideDO.getUnimodMap());
        List<AminoAcid> expect = new ArrayList<>();
        char[] temp = peptideDO.getSequence().toCharArray();
        for (int i = 0; i < 10; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(temp[i]));
            expect.add(aa);
        }
        assert (result.equals(expect));
    }

    @Test
    public void parseAminoAcidTest_1() {
        PeptideDO peptideDO = getJsonFromFileTest1();
        List<AminoAcid> result = fragmentCalculator.parseAminoAcid(peptideDO.getSequence(), peptideDO.getUnimodMap());
        List<AminoAcid> expect = new ArrayList<>();
        char[] temp = peptideDO.getSequence().toCharArray();
        for (int i = 0; i < 7; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(temp[i]));
            expect.add(aa);
        }
        assert (result.equals(expect));
    }

    @Test
    public void parseAminoAcidTest_2() {
        PeptideDO peptideDO = getJsonFromFileTest2();
        List<AminoAcid> result = fragmentCalculator.parseAminoAcid(peptideDO.getSequence(), peptideDO.getUnimodMap());
        List<AminoAcid> expect = new ArrayList<>();
        char[] temp = peptideDO.getSequence().toCharArray();
        for (int i = 0; i < 15; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(temp[i]));
            expect.add(aa);
        }
        assert (result.equals(expect));
    }

    @Test
    public void parseAminoAcidTest_3() {
        PeptideDO peptideDO = getJsonFromFileTest3();
        List<AminoAcid> result = fragmentCalculator.parseAminoAcid(peptideDO.getSequence(), peptideDO.getUnimodMap());
        List<AminoAcid> expect = new ArrayList<>();
        char[] temp = peptideDO.getSequence().toCharArray();
        for (int i = 0; i < 17; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(temp[i]));
            aa.setModId(peptideDO.getUnimodMap().get(i));
            expect.add(aa);
        }
        assert (result.equals(expect));
    }

    @Test
    public void checkTest() {
        PeptideDO peptideDO = getJsonFromFileTest();
        List<MzResult> results = fragmentCalculator.check(peptideDO.getLibraryId(), null, false);

    }

}
