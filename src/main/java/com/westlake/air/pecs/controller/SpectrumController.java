package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-08 11:28
 */
@Controller
@RequestMapping("spectrum")
public class SpectrumController extends BaseController {

    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    MzMLParser mzMLParser;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;

    @RequestMapping(value = "/mzxmlextractor")
    String mzxmlextractor(Model model,
                          @RequestParam(value = "isZlibCompression", required = false) boolean isZlibCompression,
                          @RequestParam(value = "values", required = false) String values,
                          @RequestParam(value = "precision", required = false, defaultValue = "32") Integer precision) {
        model.addAttribute("values", values);
        model.addAttribute("precision", precision);
        model.addAttribute("isZlibCompression", isZlibCompression);

        if (values != null && !values.isEmpty()) {
            MzIntensityPairs pairs = mzXMLParser.getPeakMap(new Base64().decode(values.trim()), precision, isZlibCompression);
            model.addAttribute("mzArray", pairs.getMzArray());
            model.addAttribute("intensityArray", pairs.getIntensityArray());
        }

        return "spectrum/mzxmlextractor";
    }

    @RequestMapping(value = "/mzmlextractor")
    String mzmlextractor(Model model,
                         @RequestParam(value = "isZlibCompression", required = false) boolean isZlibCompression,
                         @RequestParam(value = "mz", required = false) String mz,
                         @RequestParam(value = "intensity", required = false) String intensity,
                         @RequestParam(value = "mzPrecision", required = false, defaultValue = "32") Integer mzPrecision,
                         @RequestParam(value = "intensityPrecision", required = false, defaultValue = "32") Integer intensityPrecision) {
        model.addAttribute("mz", mz);
        model.addAttribute("intensity", intensity);
        model.addAttribute("mzPrecision", mzPrecision);
        model.addAttribute("intensityPrecision", intensityPrecision);
        model.addAttribute("isZlibCompression", isZlibCompression);

        if (mz != null && !mz.isEmpty() && intensity != null && !intensity.isEmpty()) {
            MzIntensityPairs pairs = mzMLParser.getPeakMap(new Base64().decode(mz.trim()), new Base64().decode(intensity.trim()), mzPrecision, intensityPrecision, isZlibCompression);
            if (pairs != null) {
                model.addAttribute("mzArray", pairs.getMzArray());
                model.addAttribute("intensityArray", pairs.getIntensityArray());
            } else {
                model.addAttribute(ERROR_MSG, ResultCode.EXTRACT_FAILED.getMessage());
            }
        }

        return "spectrum/mzmlextractor";
    }

    @RequestMapping(value = "/mzxmlcompressor")
    String mzxmlCompressor(Model model) {

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
        if (expResult.isFailed()) {
            resultDO.setErrorResult(ResultCode.EXPERIMENT_NOT_EXISTED);
            return resultDO;
        }

        if (indexResult.isFailed()) {
            resultDO.setErrorResult(ResultCode.SCAN_INDEX_NOT_EXISTED);
            return resultDO;
        }

        ExperimentDO experimentDO = expResult.getModel();
        ScanIndexDO scanIndexDO = indexResult.getModel();

        File file = new File(experimentDO.getFileLocation());
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZXML)) {
                pairs = mzXMLParser.parseOne(raf, scanIndexDO.getStart(), scanIndexDO.getEnd());
            } else {
                pairs = mzMLParser.parseOne(raf, scanIndexDO.getStart(), scanIndexDO.getEnd());

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
