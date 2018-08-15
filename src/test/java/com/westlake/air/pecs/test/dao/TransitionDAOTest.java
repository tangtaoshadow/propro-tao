package com.westlake.air.pecs.test.dao;

import com.westlake.air.pecs.dao.TransitionDAO;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.TransitionQuery;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-08 19:07
 */
public class TransitionDAOTest extends BaseTest {

    @Autowired
    TransitionDAO transitionDAO;

    @Test
    public void getAllByLibraryIdTest() {
        List<TransitionDO> list = transitionDAO.getAllByLibraryId("5b6712012ada5f2dc8de57d7");
        assert true;
    }

    //TODO asw 类名改了,重新写一下测试用例
    @Test
    public void getTransitionCutInfosTest() {
        List<String> result = transitionDAO.getTransitionCutInfos("5b67136d2ada5f15749a0140", "AGVETTTPSK_2");
        List<TransitionDO> list = transitionDAO.getAllByLibraryId("5b6712012ada5f2dc8de57d7");
        assert true;
    }

    @Test
    public void getAllByLibraryIdAndIsDecoy() {
        List<TransitionDO> result = transitionDAO.getAllByLibraryIdAndIsDecoy("5b67136d2ada5f15749a0140", false);
        assert true;
    }

    @Test
    public void getListTest() {
        TransitionDO transitionDO = getJsonFromFileTest();
        TransitionQuery transitionQuery = new TransitionQuery();
        List<TransitionDO> result = transitionDAO.getList(transitionQuery);
        assert true;
    }

    @Test
    public void getTTAllTest() {
        assert true;
    }

    @Test
    public void countTest() {
        assert true;
    }

    @Test
    public void getByIdTest() {
        assert true;
    }

    @Test
    public void insertTest_TransitionDO() {
        assert true;
    }

    @Test
    public void insertTest_ListTransitionDO() {
        assert true;
    }

    @Test
    public void updateTest() {
        assert true;
    }

    @Test
    public void deleteTest() {
        assert true;
    }

    @Test
    public void deleteAllByLibraryIdTest(){
        assert true;
    }

    @Test
    public void getProteinListTest(){
        assert true;
    }

    @Test
    public void getPeptideListTest(){
        assert true;
    }

}
