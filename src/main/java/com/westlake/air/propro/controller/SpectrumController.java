package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.algorithm.parser.AirdFileParser;
import com.westlake.air.propro.domain.db.SwathIndexDO;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.SwathIndexService;
import com.westlake.air.propro.utils.FileUtil;
import com.westlake.air.propro.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-08 11:28
 */
@Controller
@RequestMapping("spectrum")
public class SpectrumController extends BaseController {

    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    SwathIndexService swathIndexService;

    @RequestMapping(value = "/view")
    @ResponseBody
    ResultDO<JSONObject> view(Model model,
                              @RequestParam(value = "indexId", required = false) String indexId,
                              @RequestParam(value = "rt", required = false) float rt,
                              @RequestParam(value = "expId", required = false) String expId) {
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        MzIntensityPairs pairs = null;

        ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
        if (expResult.isFailed()) {
            resultDO.setErrorResult(ResultCode.EXPERIMENT_NOT_EXISTED);
            return resultDO;
        }
        PermissionUtil.check(expResult.getModel());

        SwathIndexDO swathIndex = swathIndexService.getById(indexId);
        if (swathIndex == null) {
            resultDO.setErrorResult(ResultCode.SWATH_INDEX_NOT_EXISTED);
            return resultDO;
        }

        ExperimentDO experimentDO = expResult.getModel();

        RandomAccessFile raf = null;
        try {
            File file = new File(experimentDO.getAirdPath());
            raf = new RandomAccessFile(file, "r");
            pairs = airdFileParser.parseValue(raf, swathIndex, rt);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(raf);
        }

        JSONObject res = new JSONObject();
        JSONArray mzArray = new JSONArray();
        JSONArray intensityArray = new JSONArray();
        if (pairs == null) {
            return ResultDO.buildError(ResultCode.DATA_IS_EMPTY);
        }

        Float[] pairMzArray = pairs.getMzArray();
        Float[] pairIntensityArray = pairs.getIntensityArray();
        for (int n = 0; n < pairMzArray.length; n++) {
            mzArray.add(pairMzArray[n]);
            intensityArray.add(pairIntensityArray[n]);
        }

        res.put("mz", mzArray);
        res.put("intensity", intensityArray);
        resultDO.setModel(res);
        return resultDO;
    }

}
