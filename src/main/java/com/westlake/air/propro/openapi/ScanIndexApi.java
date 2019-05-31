package com.westlake.air.propro.openapi;

import com.westlake.air.propro.controller.BaseController;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
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
@RequestMapping(value = "api/scanIndex")
@Api("OpenAPI 1.0-Beta for Propro")
public class ScanIndexApi extends BaseController {

    @Autowired
    ScanIndexService scanIndexService;

    @ResponseBody
    @RequestMapping(value = "getById", method = RequestMethod.GET)
    @ApiOperation(value = "Get ScanIndex by Id", notes = "根据ID获取索引对象")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "scanIndex id", dataType = "string", required = true)
    })
    public ResultDO<ScanIndexDO> getScanIndexList(Model model,
                                                  @RequestParam(value = "id", required = true) String id) {
        ResultDO<ScanIndexDO> resultDO = scanIndexService.getById(id);
        return resultDO;
    }

    @ResponseBody
    @RequestMapping(value = "getList", method = RequestMethod.GET)
    @ApiOperation(value = "Get ScanIndex List", notes = "根据条件获取索引列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "expId", value = "experiment id", dataType = "string", required = true),
            @ApiImplicitParam(name = "msLevel", value = "scanIndex ms level", dataType = "int", required = true),
            @ApiImplicitParam(name = "rtStart", value = "retention time start point", dataType = "double", required = true),
            @ApiImplicitParam(name = "rtEnd", value = "retention time end point", dataType = "double", required = true),
            @ApiImplicitParam(name = "pageSize", value = "page size", dataType = "int", required = false, defaultValue = "50"),
            @ApiImplicitParam(name = "currentPage", value = "current page", dataType = "int", required = false, defaultValue = "1")
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
        query.setExpId(expId);
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
