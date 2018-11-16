package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.MsFileType;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.parser.AirdFileParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.parser.model.traml.Configuration;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.utils.FileUtil;
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
import java.nio.ByteOrder;

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
    AirdFileParser airdFileParser;
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

            Float[] mzArray = mzXMLParser.getValues(new Base64().decode(mz.trim()), mzPrecision, isZlibCompression, ByteOrder.LITTLE_ENDIAN);
            Float[] intensityArray = mzXMLParser.getValues(new Base64().decode(intensity.trim()), intensityPrecision, isZlibCompression, ByteOrder.LITTLE_ENDIAN);

            if (mzArray == null || intensityArray == null || mzArray.length != intensityArray.length) {
                return null;
            }

            model.addAttribute("mzArray", mzArray);
            model.addAttribute("intensityArray", intensityArray);
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
                              @RequestParam(value = "type", required = false) String type,
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


        RandomAccessFile raf = null;
        try {
            if(type.equals(Constants.AIRD_FILE_TYPE_TEXT)){
                File file = new File(experimentDO.getAirdPath());
                raf = new RandomAccessFile(file, "r");
                pairs = airdFileParser.parseValueFromText(raf, scanIndexDO.getPosStart(MsFileType.AIRD), scanIndexDO.getPosEnd(MsFileType.AIRD), Constants.AIRD_COMPRESSION_TYPE_ZLIB, Constants.AIRD_PRECISION_32);
            }else{
                File file = new File(experimentDO.getAirdBinPath());
                raf = new RandomAccessFile(file, "r");
                pairs = airdFileParser.parseValueFromBin(raf, scanIndexDO.getPosStart(MsFileType.AIRD_BIN), scanIndexDO.getPosEnd(MsFileType.AIRD_BIN), Constants.AIRD_COMPRESSION_TYPE_ZLIB, Constants.AIRD_PRECISION_32);
            }
        } catch (FileNotFoundException e) {
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

    @RequestMapping(value = "/viewmzxml")
    @ResponseBody
    ResultDO<JSONObject> viewMzXML(Model model,
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

        File file = new File(experimentDO.getFilePath());

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            pairs = mzXMLParser.parseValue(raf, scanIndexDO.getPosStart(MsFileType.MZXML), scanIndexDO.getPosEnd(MsFileType.MZXML), experimentDO.getCompressionType(), experimentDO.getPrecision());

        } catch (FileNotFoundException e) {
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
//            if(pairIntensityArray[n] == 0f){
//                continue;
//            }
            mzArray.add(pairMzArray[n]);
            intensityArray.add(pairIntensityArray[n]);
        }

        res.put("mz", mzArray);
        res.put("intensity", intensityArray);
        resultDO.setModel(res);
        return resultDO;
    }


}
