package com.westlake.air.propro.openapi;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.controller.BaseController;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.query.LibraryQuery;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.service.ScanIndexService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "api/library")
@Api("OpenAPI 1.0-Beta for Propro")
public class LibraryApi extends BaseController {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;

    /**
     * 根据ID获取标准库对象(包含iRT库)
     *
     * @param model
     * @param id
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "getById", method = RequestMethod.GET)
    @ApiOperation(value = "Get Library by Id", notes = "根据ID获取标准库或者是iRT库对象")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "library id", dataType = "string", required = true)
    })
    public ResultDO<LibraryDO> getLibraryById(Model model,
                                              @RequestParam(value = "id", required = true) String id) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        return resultDO;
    }

    /**
     * 根据条件获取标准库或者是iRT库列表
     *
     * @param model
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "getList", method = RequestMethod.GET)
    @ApiOperation(value = "Get Library List", notes = "根据条件获取标准库或者是iRT库列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "library type", dataType = "int", required = false),
            @ApiImplicitParam(name = "pageSize", value = "page size", dataType = "int", required = false, defaultValue = "50"),
            @ApiImplicitParam(name = "currentPage", value = "current page", dataType = "int", required = false, defaultValue = "1")
    })
    public ResultDO<List<LibraryDO>> getLibraryList(Model model,
                                                    @RequestParam(value = "type", required = false, defaultValue = "1") Integer type,
                                                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                                                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize) {
        LibraryQuery libraryQuery = new LibraryQuery();
        if (type != null) {
            libraryQuery.setType(type);
        }

        buildPageQuery(libraryQuery, currentPage, pageSize);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(libraryQuery);
        return resultDO;
    }

    /**
     * 根据LibraryId和其余条件获取该标准库中的肽段信息
     *
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
    @RequestMapping(value = "peptide/getList", method = RequestMethod.GET)
    @ApiOperation(value = "Get Peptide List", notes = "根据条件获取标准库中的肽段列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "libraryId", value = "library id", dataType = "string", required = true),
            @ApiImplicitParam(name = "isDecoy", value = "is decoy or target", dataType = "boolean", required = false),
            @ApiImplicitParam(name = "mzStart", value = "precursor mz start", dataType = "double", required = false),
            @ApiImplicitParam(name = "mzEnd", value = "precursor mz end", dataType = "double", required = false),
            @ApiImplicitParam(name = "pageSize", value = "page size", dataType = "int", required = false, defaultValue = "1"),
            @ApiImplicitParam(name = "currentPage", value = "current page", dataType = "int", required = false, defaultValue = "50")
    })
    public ResultDO<List<PeptideDO>> getPeptideList(Model model,
                                                    @RequestParam(value = "libraryId", required = true) String libraryId,
                                                    @RequestParam(value = "isDecoy", required = false) Boolean isDecoy,
                                                    @RequestParam(value = "mzStart", required = false) Double mzStart,
                                                    @RequestParam(value = "mzEnd", required = false) Double mzEnd,
                                                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                                                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize) {
        PeptideQuery query = new PeptideQuery();
        if (libraryId != null) {
            query.setLibraryId(libraryId);
        }
        if (isDecoy != null) {
            query.setIsDecoy(isDecoy);
        }
        if (mzStart != null) {
            query.setMzStart(mzStart);
        }
        if (mzEnd != null) {
            query.setMzEnd(mzEnd);
        }

        buildPageQuery(query, currentPage, pageSize);
        ResultDO<List<PeptideDO>> resultDO = peptideService.getList(query);
        return resultDO;
    }

    /**
     * 根据LibraryId和PeptideRef获取Peptide对象
     *
     * @param model
     * @param libraryId
     * @param isDecoy
     * @param peptideRef
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "peptide/getByPeptideRefAndIsDecoy", method = RequestMethod.GET)
    @ApiOperation(value = "Get Peptide by PeptideRef and IsDecoy", notes = "根据PeptideRef和是否是伪肽段来获取肽段对象")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "libraryId", value = "library id", dataType = "string", required = true),
            @ApiImplicitParam(name = "isDecoy", value = "is decoy or target", dataType = "boolean", required = true),
            @ApiImplicitParam(name = "peptideRef", value = "peptide ref", dataType = "string", required = true)
    })
    public ResultDO<PeptideDO> getByPeptideRefAndIsDecoy(Model model,
                                                         @RequestParam(value = "libraryId", required = true) String libraryId,
                                                         @RequestParam(value = "isDecoy", required = true) Boolean isDecoy,
                                                         @RequestParam(value = "peptideRef", required = true) String peptideRef) {

        PeptideDO peptide = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy(libraryId, peptideRef, isDecoy);
        if (peptide == null) {
            return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
        }
        ResultDO resultDO = new ResultDO(true);
        resultDO.setModel(peptide);
        return resultDO;
    }

    /**
     * 根据ID获取索引对象
     *
     * @param model
     * @param id
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "scanIndex/getById", method = RequestMethod.GET)
    @ApiOperation(value = "Get ScanIndex by Id", notes = "根据ID获取索引对象")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "scanIndex id", dataType = "string", required = true)
    })
    public ResultDO<ScanIndexDO> getScanIndexList(Model model,
                                                  @RequestParam(value = "id", required = true) String id) {
        ResultDO<ScanIndexDO> resultDO = scanIndexService.getById(id);
        return resultDO;
    }

    /**
     * 根据ID获取标准库对象(包含iRT库)
     *
     * @param model
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "scanIndex/getList", method = RequestMethod.GET)
    @ApiOperation(value = "Get ScanIndex List", notes = "根据条件获取索引列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "expId", value = "experiment id", dataType = "string", required = true),
            @ApiImplicitParam(name = "msLevel", value = "scanIndex ms level", dataType = "int", required = true),
            @ApiImplicitParam(name = "rtStart", value = "retention time start point", dataType = "double", required = true),
            @ApiImplicitParam(name = "rtEnd", value = "retention time end point", dataType = "double", required = true),
            @ApiImplicitParam(name = "pageSize", value = "page size", dataType = "int", required = false, defaultValue = "1"),
            @ApiImplicitParam(name = "currentPage", value = "current page", dataType = "int", required = false, defaultValue = "50")
    })
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
        if (rtStart != null) {
            query.setRtStart(rtStart);
        }
        if (rtEnd != null) {
            query.setRtEnd(rtEnd);
        }
        buildPageQuery(query, currentPage, pageSize);
        ResultDO<List<ScanIndexDO>> resultDO = scanIndexService.getList(query);

        return resultDO;
    }
}
