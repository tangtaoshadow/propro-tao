package com.westlake.air.pecs.test.algorithm;

import com.westlake.air.pecs.algorithm.FormulaCalculator;
import com.westlake.air.pecs.algorithm.FragmentCalculator;
import com.westlake.air.pecs.domain.bean.transition.AminoAcid;
import com.westlake.air.pecs.domain.bean.transition.Fragment;
import com.westlake.air.pecs.domain.bean.transition.FragmentResult;
import com.westlake.air.pecs.domain.bean.analyse.MzResult;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.service.TransitionService;
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
    TransitionService transitionService;
    @Autowired
    FormulaCalculator formulaCalculator;

    @Test
    public void getFragmentsTest() {
        TransitionDO transitionDO = getJsonFromFileTest();
        Fragment fragment = fragmentCalculator.getFragment(transitionDO);
        List<Fragment> expect = new ArrayList<>();

        assert (fragment.equals(expect));
    }

    @Test
    public void getBaseFragmentsTest() {
        TransitionDO transitionDO = getJsonFromFileTest();
        Fragment result = fragmentCalculator.getBaseFragment(transitionDO);
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
        TransitionDO transitionDO = getJsonFromFileTest1();
        Fragment result = fragmentCalculator.getBaseFragment(transitionDO);
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
        TransitionDO transitionDO = getJsonFromFileTest2();
        Fragment result = fragmentCalculator.getBaseFragment(transitionDO);
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
        TransitionDO transitionDO = getJsonFromFileTest3();
        Fragment result = fragmentCalculator.getBaseFragment(transitionDO);
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
        TransitionDO transitionDO = getJsonFromFileTest4();
        Fragment result = fragmentCalculator.getBaseFragment(transitionDO);
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
        TransitionDO transitionDO = getJsonFromFileTest();
        String result = fragmentCalculator.getFragmentSequence(transitionDO.getSequence(), transitionDO.getAnnotation().getType(), transitionDO.getAnnotation().getLocation());
        String expect = "AGVETTTPSK".substring(3, 10);
        assert (result.equals(expect));
    }

    @Test
    public void getFragmentSequenceTest_2() {
        TransitionDO transitionDO = getJsonFromFileTest3();
        String result = fragmentCalculator.getFragmentSequence(transitionDO.getSequence(), transitionDO.getAnnotation().getType(), transitionDO.getAnnotation().getLocation());
        String expect = "VHTECCHGDLLECADDR".substring(0, 10);
        assert (result.equals(expect));
    }

    @Test
    public void getFragmentSequenceTest_3() {
        TransitionDO transitionDO = getJsonFromFileTest2();
        String result = fragmentCalculator.getFragmentSequence(transitionDO.getSequence(), transitionDO.getAnnotation().getType(), transitionDO.getAnnotation().getLocation());
        String expect = "KVPQVSTPTLVEVSR".substring(10, 15);
        assert (result.equals(expect));
    }

    @Test
    public void getFragmentSequenceTest_4() {
        TransitionDO transitionDO = getJsonFromFileTest4();
        String result = fragmentCalculator.getFragmentSequence(transitionDO.getSequence(), transitionDO.getAnnotation().getType(), transitionDO.getAnnotation().getLocation());
        String expect = "TCVADESAENCDK".substring(2, 13);
        assert (result.equals(expect));
    }

    @Test
    public void decoyOverviewTest() {
        TransitionDO transition = getJsonFromFileTest2();
        FragmentResult result = fragmentCalculator.decoyOverview(transition.getLibraryId());
    }

    @Test
    public void parseAminoAcidTest() {
        TransitionDO transitionDO = getJsonFromFileTest();
        List<AminoAcid> result = fragmentCalculator.parseAminoAcid(transitionDO.getSequence(), transitionDO.getUnimodMap());
        List<AminoAcid> expect = new ArrayList<>();
        char[] temp = transitionDO.getSequence().toCharArray();
        for (int i = 0; i < 10; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(temp[i]));
            expect.add(aa);
        }
        assert (result.equals(expect));
    }

    @Test
    public void parseAminoAcidTest_1() {
        TransitionDO transitionDO = getJsonFromFileTest1();
        List<AminoAcid> result = fragmentCalculator.parseAminoAcid(transitionDO.getSequence(), transitionDO.getUnimodMap());
        List<AminoAcid> expect = new ArrayList<>();
        char[] temp = transitionDO.getSequence().toCharArray();
        for (int i = 0; i < 7; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(temp[i]));
            expect.add(aa);
        }
        assert (result.equals(expect));
    }

    @Test
    public void parseAminoAcidTest_2() {
        TransitionDO transitionDO = getJsonFromFileTest2();
        List<AminoAcid> result = fragmentCalculator.parseAminoAcid(transitionDO.getSequence(), transitionDO.getUnimodMap());
        List<AminoAcid> expect = new ArrayList<>();
        char[] temp = transitionDO.getSequence().toCharArray();
        for (int i = 0; i < 15; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(temp[i]));
            expect.add(aa);
        }
        assert (result.equals(expect));
    }

    @Test
    public void parseAminoAcidTest_3() {
        TransitionDO transitionDO = getJsonFromFileTest3();
        List<AminoAcid> result = fragmentCalculator.parseAminoAcid(transitionDO.getSequence(), transitionDO.getUnimodMap());
        List<AminoAcid> expect = new ArrayList<>();
        char[] temp = transitionDO.getSequence().toCharArray();
        for (int i = 0; i < 17; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(temp[i]));
            aa.setModId(transitionDO.getUnimodMap().get(i));
            expect.add(aa);
        }
        assert (result.equals(expect));
    }

    @Test
    public void checkTest() {
        TransitionDO transitionDO = getJsonFromFileTest();
        List<MzResult> results = fragmentCalculator.check(transitionDO.getLibraryId(), null, false);

    }

}
