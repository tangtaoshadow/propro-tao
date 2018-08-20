package com.westlake.air.pecs.test.library;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.TransitionTraMLParser;
import com.westlake.air.pecs.parser.model.traml.Transition;
import com.westlake.air.pecs.service.LibraryService;
import com.westlake.air.pecs.service.TransitionService;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-20 14:11
 */
public class LibraryTest extends BaseTest {

    @Autowired
    MzMLParser mzMLParser;
    @Autowired
    TransitionTraMLParser traMLParser;
    @Autowired
    LibraryService libraryService;
    @Autowired
    TransitionService transitionService;

    @Test
    public void extractor_tsv_parser_Test_1() throws Exception {
        LibraryDO libraryDO = new LibraryDO();
        libraryDO.setName("测试用临时库");
        libraryDO.setType(LibraryDO.TYPE_STANDARD);
        libraryService.insert(libraryDO);
        String filePath = getClass().getClassLoader().getResource("ChromatogramExtractor_input.tsv").getPath();
        File file = new File(filePath);
        ResultDO resultDO = libraryService.parseAndInsert(libraryDO, new FileInputStream(file), filePath, true, new TaskDO());
        assert resultDO.isSuccess();
        List<TransitionDO> trans = transitionService.getAllByLibraryId(libraryDO.getId());
        assert trans.size() == 3;

        transitionService.deleteAllByLibraryId(libraryDO.getId());
        libraryService.delete(libraryDO.getId());
    }

}
