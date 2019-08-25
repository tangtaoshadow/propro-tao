package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.algorithm.decoy.generator.ShuffleGenerator;
import com.westlake.air.propro.algorithm.parser.*;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.constants.enums.TaskStatus;
import com.westlake.air.propro.dao.LibraryDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.LibraryQuery;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.TaskService;
import com.westlake.air.propro.service.PeptideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:45
 */
@Service("libraryService")
public class LibraryServiceImpl implements LibraryService {

    public final Logger logger = LoggerFactory.getLogger(LibraryServiceImpl.class);

    int errorListNumberLimit = 10;

    @Autowired
    LibraryDAO libraryDAO;
    @Autowired
    PeptideService peptideService;
    @Autowired
    LibraryTsvParser tsvParser;
    @Autowired
    TraMLParser traMLParser;
    @Autowired
    FastTraMLParser fastTraMLParser;
    @Autowired
    MsmsParser msmsParser;
    @Autowired
    TaskService taskService;
    @Autowired
    FastaParser fastaParser;
    @Autowired
    ShuffleGenerator shuffleGenerator;

    @Override
    public List<LibraryDO> getSimpleAll(String username, Integer type, Boolean doPublic) {
        return libraryDAO.getSimpleAll(username, type, doPublic);
    }

    @Override
    public List<LibraryDO> getAllPublic(Integer type) {
        return libraryDAO.getPublicSimpleAll(type);
    }

    @Override
    public long count(LibraryQuery query) {
        return libraryDAO.count(query);
    }

    @Override
    public ResultDO<List<LibraryDO>> getList(LibraryQuery query) {
        List<LibraryDO> libraryDOS = libraryDAO.getList(query);
        long totalCount = libraryDAO.count(query);
        ResultDO<List<LibraryDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(libraryDOS);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public List<LibraryDO> getAll(LibraryQuery query) {
        return libraryDAO.getAll(query);
    }

    @Override
    public ResultDO insert(LibraryDO libraryDO) {
        if (libraryDO.getName() == null || libraryDO.getName().isEmpty()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }
        try {
            libraryDO.setCreateDate(new Date());
            libraryDO.setLastModifiedDate(new Date());
            libraryDAO.insert(libraryDO);
            return ResultDO.build(libraryDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            if (e.getMessage().contains("E11000")) {
                return ResultDO.buildError(ResultCode.DUPLICATE_KEY_ERROR);
            } else {
                return ResultDO.buildError(ResultCode.INSERT_ERROR);
            }
        }
    }

    @Override
    public ResultDO update(LibraryDO libraryDO) {
        if (libraryDO.getId() == null || libraryDO.getId().isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (libraryDO.getName() == null || libraryDO.getName().isEmpty()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }

        try {
            libraryDO.setLastModifiedDate(new Date());
            libraryDAO.update(libraryDO);
            return ResultDO.build(libraryDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO delete(String id) {
        if (id == null || id.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            libraryDAO.delete(id);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public LibraryDO getById(String id) {

        try {
            return libraryDAO.getById(id);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return null;
        }
    }

    @Override
    public LibraryDO getByName(String name) {

        try {
            return libraryDAO.getByName(name);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return null;
        }
    }

    @Override
    public String getNameById(String id) {
        LibraryDO libraryDO = libraryDAO.getById(id);
        if (libraryDO != null) {
            return libraryDO.getName();
        }
        return null;
    }

    @Override
    public ResultDO parseAndInsert(LibraryDO library, InputStream libFileStream, String fileName, InputStream prmFileStream, TaskDO taskDO) {

        ResultDO resultDO;

        //parse prm
        HashMap<String, PeptideDO> prmPeptideRefMap = new HashMap<>();
        if (prmFileStream != null) {
            try {
                ResultDO<HashMap<String, PeptideDO>> prmResultDO = tsvParser.getPrmPeptideRef(prmFileStream);
                if (prmResultDO.isFailed()) {
                    logger.warn(prmResultDO.getMsgInfo());
                }
                prmPeptideRefMap = prmResultDO.getModel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if (fileName.toLowerCase().endsWith("tsv") || fileName.toLowerCase().endsWith("csv")) {
            if (prmPeptideRefMap.isEmpty()) {
                resultDO = tsvParser.parseAndInsert(libFileStream, library, taskDO);
            } else {
                resultDO = tsvParser.selectiveParseAndInsert(libFileStream, library, new HashSet<>(prmPeptideRefMap.keySet()), false, taskDO);
            }
        } else if (fileName.toLowerCase().endsWith("traml")) {
            if (prmPeptideRefMap.isEmpty()) {
                resultDO = fastTraMLParser.parseAndInsert(libFileStream, library, taskDO);
            } else {
                resultDO = fastTraMLParser.selectiveParseAndInsert(libFileStream, library, new HashSet<>(prmPeptideRefMap.keySet()), false, taskDO);
            }
        } else if (fileName.toLowerCase().endsWith("txt")) {
            if (prmPeptideRefMap.isEmpty()) {
                resultDO = msmsParser.parseAndInsert(libFileStream, library, taskDO);
            } else {
                resultDO = msmsParser.selectiveParseAndInsert(libFileStream, library, new HashSet<>(prmPeptideRefMap.keySet()), false, taskDO);
            }
        } else {
            return ResultDO.buildError(ResultCode.INPUT_FILE_TYPE_MUST_BE_TSV_OR_TRAML);
        }
        library.setGenerator(ShuffleGenerator.NAME);
        return resultDO;
    }

    @Override
    public void countAndUpdateForLibrary(LibraryDO library) {
        try {
            library.setProteinCount(peptideService.countByProteinName(library.getId()));
            library.setUniqueProteinCount(peptideService.countByUniqueProteinName(library.getId()));

            PeptideQuery query = new PeptideQuery();
            query.setLibraryId(library.getId());
            library.setTotalCount(peptideService.count(query));
            query.setIsUnique(true);
            library.setTotalUniqueCount(peptideService.count(query));

            update(library);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void uploadFile(LibraryDO library, InputStream libFileStream, String fileName, InputStream prmFileStream, TaskDO taskDO) {
        //先Parse文件,再作数据库的操作
        ResultDO result = parseAndInsert(library, libFileStream, fileName, prmFileStream, taskDO);
        if (result.getErrorList() != null) {
            if (result.getErrorList().size() > errorListNumberLimit) {
                taskDO.addLog("解析错误,错误的条数过多,这边只显示" + errorListNumberLimit + "条错误信息");
                taskDO.addLog(result.getErrorList().subList(0, errorListNumberLimit));
            } else {
                taskDO.addLog(result.getErrorList());
            }
        }

        if (result.isFailed()) {
            taskDO.addLog(result.getMsgInfo());
            taskDO.finish(TaskStatus.FAILED.getName());
        }

        /**
         * 如果全部存储成功,开始统计蛋白质数目,肽段数目和Transition数目
         */
        taskDO.addLog("开始统计蛋白质数目,肽段数目和Transition数目");
        taskService.update(taskDO);
        countAndUpdateForLibrary(library);

        taskDO.addLog("统计完毕");
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }
}
