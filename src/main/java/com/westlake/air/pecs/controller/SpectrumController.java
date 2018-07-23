package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.parser.MzXmlParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-08 11:28
 */
@Controller
@RequestMapping("spectrum")
public class SpectrumController {

    @Autowired
    MzXmlParser mzXmlParser;

    @Autowired
    ExperimentService experimentService;

    @Autowired
    ScanIndexService scanIndexService;

    @RequestMapping(value = "/mzxmlextractor")
    String mzxmlextractor(Model model,
                          @RequestParam(value = "isCompression", required = false) boolean isCompression,
                          @RequestParam(value = "values", required = false) String values,
                          @RequestParam(value = "precision", required = false, defaultValue = "32") Integer precision) {
        model.addAttribute("values", values);
        model.addAttribute("precision", precision);
        model.addAttribute("isCompression", isCompression);

        if (values != null && !values.isEmpty()) {
            Map<Double, Double> peakMap = mzXmlParser.getPeakMap(values.trim(), precision, isCompression);
            model.addAttribute("peakMap", peakMap);
        }

        return "spectrum/mzxmlextractor";
    }

    @RequestMapping(value = "/mzmlextractor")
    String mzmlextractor(Model model,
                         @RequestParam(value = "isCompression", required = false) boolean isCompression,
                         @RequestParam(value = "mz", required = false) String mz,
                         @RequestParam(value = "intensity", required = false) String intensity,
                         @RequestParam(value = "mzPrecision", required = false, defaultValue = "32") Integer mzPrecision,
                         @RequestParam(value = "intensityPrecision", required = false, defaultValue = "32") Integer intensityPrecision) {
        model.addAttribute("mz", mz);
        model.addAttribute("intensity", intensity);
        model.addAttribute("mzPrecision", mzPrecision);
        model.addAttribute("intensityPrecision", intensityPrecision);

        if (mz != null && !mz.isEmpty() && intensity != null && !intensity.isEmpty()) {
            Map<Double, Double> peakMap = mzXmlParser.getPeakMap(mz.trim(), intensity.trim(), mzPrecision, intensityPrecision, isCompression);
            model.addAttribute("peakMap", peakMap);
        }

        return "spectrum/mzmlextractor";
    }

    @RequestMapping(value = "/view")
    @ResponseBody
    ResultDO<JSONObject> view(Model model,
                              @RequestParam(value = "indexId", required = false) String indexId,
                              @RequestParam(value = "expId", required = false) String expId) {

        ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
        ResultDO<ScanIndexDO> indexResult = scanIndexService.getById(indexId);

        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        MzIntensityPairs pairs = null;
        if (expResult.isFailured()) {
            resultDO.setErrorResult(ResultCode.EXPERIMENT_NOT_EXISTED);
            return resultDO;
        }

        if (indexResult.isFailured()) {
            resultDO.setErrorResult(ResultCode.SCAN_INDEX_NOT_EXISTED);
            return resultDO;
        }

        ExperimentDO experimentDO = expResult.getModel();
        ScanIndexDO scanIndexDO = indexResult.getModel();

        File file = new File(experimentDO.getFileLocation());
        pairs = mzXmlParser.parseOne(file, scanIndexDO);

        JSONObject res = new JSONObject();
        JSONArray mzArray = new JSONArray();
        JSONArray intensityArray = new JSONArray();
        if (pairs == null) {
            return ResultDO.buildError(ResultCode.DATA_IS_EMPTY);
        }

        Double[] pairMzArray = pairs.getMzArray();
        Double[] pairIntensityArray = pairs.getIntensityArray();
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
