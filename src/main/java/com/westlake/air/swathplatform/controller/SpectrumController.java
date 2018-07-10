package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.parser.MzXmlParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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


}
