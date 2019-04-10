package com.westlake.air.propro.controller;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.LibraryQuery;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.service.ScanIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(value = "api")
public class ApiController extends BaseController{

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;

    /**
     * 根据ID获取实验对象
     * @param model
     * @param id
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "experiment/getById")
    public ResultDO<ExperimentDO> getExperimentById(Model model,
                                         @RequestParam(value = "id", required = true) String id) {
        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        return resultDO;
    }

    /**
     * 根据条件批量分页获取实验列表
     * @param model
     * @param currentPage
     * @param pageSize
     * @param projectName
     * @param batchName
     * @param expName
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "experiment/getList")
    public ResultDO<List<ExperimentDO>> getExperimentList(Model model,
                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                    @RequestParam(value = "projectName", required = false) String projectName,
                    @RequestParam(value = "batchName", required = false) String batchName,
                    @RequestParam(value = "expName", required = false) String expName) {
        ExperimentQuery query = new ExperimentQuery();
        if (expName != null && !expName.isEmpty()) {
            query.setName(expName);
        }
        if(projectName != null && !projectName.isEmpty()){
            query.setProjectName(projectName);
        }
        if(batchName != null && !batchName.isEmpty()){
            query.setBatchName(batchName);
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<ExperimentDO>> resultDO = experimentService.getList(query);

        return resultDO;
    }

    /**
     * 根据ID获取标准库对象(包含iRT库)
     * @param model
     * @param id
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "library/getById")
    public ResultDO<LibraryDO> getLibraryById(Model model,
                                              @RequestParam(value = "id", required = true) String id) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        return resultDO;
    }

    /**
     * 根据条件获取标准库或者是iRT库列表
     * @param model
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "library/getList")
    public ResultDO<List<LibraryDO>> getLibraryById(Model model,
                                                    @RequestParam(value = "type", required = false, defaultValue = "1") Integer type,
                                                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                                                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize) {
        LibraryQuery libraryQuery = new LibraryQuery();
        if(type != null){
            libraryQuery.setType(type);
        }

        buildPageQuery(libraryQuery, currentPage, pageSize);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(libraryQuery);
        return resultDO;
    }

    /**
     * 根据LibraryId和其余条件获取该标准库中的肽段信息
     * @param model
     * @param libraryId
     * @param isDecoy
     * @param mzStart
     * @param mzEnd
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "peptide/getList")
    public ResultDO<List<PeptideDO>> getPeptideList(Model model,
                                                    @RequestParam(value = "libraryId", required = true) String libraryId,
                                                    @RequestParam(value = "isDecoy", required = false) Boolean isDecoy,
                                                    @RequestParam(value = "mzStart", required = false) Double mzStart,
                                                    @RequestParam(value = "mzEnd", required = false) Double mzEnd,
                                                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                                                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize) {
        PeptideQuery query = new PeptideQuery();
        if(libraryId != null){
            query.setLibraryId(libraryId);
        }
        if(isDecoy != null){
            query.setIsDecoy(isDecoy);
        }
        if(mzStart != null){
            query.setMzStart(mzStart);
        }
        if(mzEnd != null){
            query.setMzEnd(mzEnd);
        }

        buildPageQuery(query, currentPage, pageSize);
        ResultDO<List<PeptideDO>> resultDO = peptideService.getList(query);
        return resultDO;
    }

    /**
     * 根据LibraryId和PeptideRef获取Peptide对象
     * @param model
     * @param libraryId
     * @param isDecoy
     * @param peptideRef
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "peptide/getByPeptideRefAndIsDecoy")
    public ResultDO<PeptideDO> getByPeptideRefAndIsDecoy(Model model,
                                                    @RequestParam(value = "libraryId", required = true) String libraryId,
                                                    @RequestParam(value = "isDecoy", required = true) Boolean isDecoy,
                                                    @RequestParam(value = "peptideRef", required = true) String peptideRef) {

        PeptideDO peptide = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy(libraryId, peptideRef, isDecoy);
        if(peptide == null){
            return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
        }
        ResultDO resultDO = new ResultDO(true);
        resultDO.setModel(peptide);
        return resultDO;
    }

    /**
     * 根据ID获取索引对象
     * @param model
     * @param id
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "scanIndex/getById")
    public ResultDO<ScanIndexDO> getScanIndexList(Model model,
                                                      @RequestParam(value = "id", required = true) String id) {
        ResultDO<ScanIndexDO> resultDO = scanIndexService.getById(id);
        return resultDO;
    }

    /**
     * 根据ID获取标准库对象(包含iRT库)
     * @param model
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "scanIndex/getList")
    public ResultDO<List<ScanIndexDO>> getScanIndexList(Model model,
                                                    @RequestParam(value = "expId", required = true) String expId,
                                                    @RequestParam(value = "msLevel", required = true) Integer msLevel,
                                                    @RequestParam(value = "rtStart", required = true) Double rtStart,
                                                    @RequestParam(value = "rtEnd", required = true) Double rtEnd,
                                                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                                                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize) {
        ScanIndexQuery query = new ScanIndexQuery();
        query.setMsLevel(msLevel);
        query.setExperimentId(expId);
        if(rtStart != null){
            query.setRtStart(rtStart);
        }
        if(rtEnd != null){
            query.setRtEnd(rtEnd);
        }
        buildPageQuery(query, currentPage, pageSize);
        ResultDO<List<ScanIndexDO>> resultDO = scanIndexService.getList(query);

        return resultDO;
    }
}
