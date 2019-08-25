package com.westlake.air.propro.openapi;

import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.controller.BaseController;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.query.LibraryQuery;
import com.westlake.air.propro.service.LibraryService;
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
    LibraryService libraryService;

    @ResponseBody
    @RequestMapping(value = "getById", method = RequestMethod.GET)
    @ApiOperation(value = "Get Library by Id", notes = "根据ID获取标准库或者是iRT库对象")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "library id", dataType = "string", required = true)
    })
    public ResultDO<LibraryDO> getById(Model model,
                                              @RequestParam(value = "id", required = true) String id) {
        LibraryDO library = libraryService.getById(id);
        if(library == null){
            return ResultDO.buildError(ResultCode.LIBRARY_NOT_EXISTED);
        }
        ResultDO<LibraryDO> resultDO = new ResultDO<>(true);
        resultDO.setModel(library);
        return resultDO;
    }

    @ResponseBody
    @RequestMapping(value = "getList", method = RequestMethod.GET)
    @ApiOperation(value = "Get Library List", notes = "根据条件获取标准库或者是iRT库列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "library type", dataType = "int", required = false),
            @ApiImplicitParam(name = "pageSize", value = "page size", dataType = "int", required = false, defaultValue = "50"),
            @ApiImplicitParam(name = "currentPage", value = "current page", dataType = "int", required = false, defaultValue = "1")
    })
    public ResultDO<List<LibraryDO>> getList(Model model,
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
}
