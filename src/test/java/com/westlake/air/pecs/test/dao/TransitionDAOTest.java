package com.westlake.air.pecs.test.dao;

import com.westlake.air.pecs.domain.db.TransitionDO;
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
    public void 测试根据LibraryId获取所有Transition列表() {
        List<TransitionDO> list = transitionDAO.getAllByLibraryId("5b6712012ada5f2dc8de57d7");
        assert(list.size()>0);
    }
}
