package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = "charts")
@Deprecated
public class ChartsController extends BaseController {

    @RequestMapping(value = "/readSpeed")
    String fileReadSpeed(Model model) {
        return "charts/filereadspeed";
    }

    @RequestMapping(value = "/getFileReadSpeedData")
    @ResponseBody
    String getFileReadSpeedData(Model model) {
        JSONArray array = new JSONArray();

        float[] vendor_HYE110_TTOF6600_32fix_lgillet_I160308_001 = new float[]{
                23.6f, 27.8f, 31.2f, 34.3f, 37.8f, 39.8f, 41.1f, 43.1f, 44.3f, 45.2f,
                46.1f, 46.3f, 46.4f, 46.4f, 46.8f, 45.8f, 45.0f, 43.6f, 41.6f, 39.2f,
                37.7f, 35.1f, 36.0f, 34.2f, 34.0f, 31.0f, 29.9f, 29.5f, 25.8f, 24.1f,
                21.6f, 19.6f};

        float[] aird_HYE110_TTOF6600_32fix_lgillet_I160308_001 = new float[]{
                3.4f, 3.6f, 4.4f, 4.3f, 4.6f, 5.9f, 6.6f, 7.2f, 5.2f, 5.6f,
                5.9f, 5.6f, 5.6f, 5.6f, 5.6f, 6.0f, 5.1f, 5.4f, 5.0f, 4.6f,
                4.5f, 4.3f, 4.0f, 6.0f, 4.2f, 3.6f, 3.7f, 3.3f, 3.1f, 2.7f,
                2.5f, 2.3f};
        float[] mzXML_HYE110_TTOF6600_32fix_lgillet_I160308_001 = new float[]{
                39.8f, 52.9f, 50.8f, 45.3f, 17.9f, 54.5f, 32.1f, 49.6f, 43.9f, 16.3f,
                52.5f, 52.0f, 38.2f, 15.5f, 23.4f, 52.7f, 55.4f, 25.7f, 27.8f, 42.6f,
                51.4f, 53.5f, 23.6f, 28.8f, 44.6f, 47.3f, 54.0f, 23.7f, 30.0f, 48.1f,
                46.8f, 54.7f,
        };



        float[] vendor_HYE110_TTOF6600_32var_lgillet_I160309_001 = new float[]{
                17.1f, 17.6f, 20.6f, 21.1f, 22.1f, 22.7f, 23.8f, 27.6f, 29.1f, 29.3f,
                27.1f, 25.7f, 25.8f, 25.6f, 25.6f, 31.2f, 32.8f, 32.7f, 34.8f, 35.3f,
                37.1f, 38.4f, 38.9f, 42.5f, 49.3f, 50.6f, 49.4f, 56.4f, 46.5f, 54.5f,
                64.1f, 74.0f};

        float[] aird_HYE110_TTOF6600_32var_lgillet_I160309_001 = new float[]{
                2.6f, 2.4f, 3.4f, 2.0f, 2.2f, 1.9f, 2.0f, 3.1f, 3.0f, 3.1f,
                3.2f, 2.7f, 3.1f, 2.9f, 2.9f, 3.5f, 3.6f, 4.0f, 3.8f, 4.0f,
                4.3f, 4.6f, 4.3f, 4.3f, 4.9f, 5.2f, 5.1f, 7.0f, 5.6f, 7.7f,
                7.7f, 9.9f};
        float[] mzXML_HYE110_TTOF6600_32var_lgillet_I160309_001 = new float[]{
                33.8f, 38.0f, 72.0f, 53.2f, 24.8f, 41.3f, 21.6f, 55.5f, 28.4f, 44.2f,
                40.4f, 21.1f, 65.8f, 47.9f, 39.1f, 39.3f, 29.6f, 25.2f, 33.4f, 28.8f,
                50.3f, 56.1f, 39.3f, 29.6f, 47.6f, 34.0f, 42.6f, 18.8f, 33.8f, 50.2f,
                19.6f, 40.0f,
        };


        float[] vendor_napedro_L120420_010_SW = new float[]{
                29.4f, 32.9f, 35.4f, 37.6f, 39.7f, 41.0f, 41.8f, 42.3f, 43.1f, 43.7f,
                43.7f, 42.3f, 42.3f, 41.4f, 40.4f, 39.0f, 37.7f, 36.3f, 35.2f, 33.4f,
                32.3f, 30.7f, 28.6f, 26.8f, 25.7f, 23.3f, 22.1f, 21.4f, 19.9f, 18.6f,
                17.5f, 16.2f};
        float[] aird_napedro_L120420_010_SW = new float[]{
                4.4f, 4.4f, 5.2f, 5.2f, 6.2f, 7.1f, 6.1f, 6.0f, 5.6f, 7.1f,
                6.6f, 6.0f, 7.1f, 5.3f, 4.9f, 4.5f, 4.2f, 4.1f, 4.1f, 4.0f,
                3.7f, 3.7f, 3.4f, 3.1f, 2.9f, 2.5f, 2.5f, 2.1f, 2.2f, 1.9f,
                1.8f, 1.7f};
        float[] mzXML_napedro_L120420_010_SW = new float[]{
                32.5f, 40.4f, 47.9f, 36.9f, 17.4f, 47.6f, 34.8f, 44.8f, 36.4f, 16.1f,
                47.5f, 49.8f, 36.0f, 15.0f, 26.4f, 47.2f, 49.9f, 20.7f, 24.6f, 44.4f,
                45.2f, 51.7f, 23.7f, 24.7f, 46.1f, 38.9f, 50.2f, 22.8f, 25.9f, 46.9f,
                36.3f, 49.2f
        };


        float[] vendor_napedro_L120224_010_SW = new float[]{
                7.5f, 8.6f, 8.0f, 8.0f, 8.4f, 7.7f, 6.4f, 6.1f, 5.5f, 5.8f,
                5.6f, 4.6f, 4.1f, 4.1f, 2.9f, 2.8f, 2.3f, 1.8f, 1.8f, 1.6f,
                1.3f, 1.4f, 1.4f, 1.1f, 1.2f, 1.0f, 0.8f, 0.9f, 0.8f, 0.7f,
                0.7f, 0.7f};
        float[] aird_napedro_L120224_010_SW = new float[]{
                0.9f, 0.9f, 1.0f, 0.9f, 0.9f, 0.9f, 0.8f, 0.9f, 0.6f, 0.5f,
                0.6f, 0.4f, 0.4f, 0.3f, 0.2f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f,
                0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f,
                0.1f, 0.1f};
        float[] mzXML_napedro_L120224_010_SW = new float[]{
                2.2f, 7.0f, 9.1f, 0.6f, 0.7f, 2.3f, 6.8f, 5.6f, 0.5f, 0.5f,
                1.9f, 5.3f, 0.5f, 0.5f, 6.9f, 1.9f, 4.7f, 1.7f, 4.8f, 6.4f,
                1.4f, 4.5f, 1.4f, 4.0f, 6.2f, 1.3f, 3.4f, 1.0f, 3.5f, 6.5f,
                0.9f, 2.7f,
        };



        List<float[]> totalDatas = new ArrayList<>();
        totalDatas.add(vendor_HYE110_TTOF6600_32fix_lgillet_I160308_001);
        totalDatas.add(aird_HYE110_TTOF6600_32fix_lgillet_I160308_001);
        totalDatas.add(mzXML_HYE110_TTOF6600_32fix_lgillet_I160308_001);

        totalDatas.add(vendor_HYE110_TTOF6600_32var_lgillet_I160309_001);
        totalDatas.add(aird_HYE110_TTOF6600_32var_lgillet_I160309_001);
        totalDatas.add(mzXML_HYE110_TTOF6600_32var_lgillet_I160309_001);

        totalDatas.add(vendor_napedro_L120224_010_SW);
        totalDatas.add(aird_napedro_L120224_010_SW);
        totalDatas.add(mzXML_napedro_L120224_010_SW);

        totalDatas.add(vendor_napedro_L120420_010_SW);
        totalDatas.add(aird_napedro_L120420_010_SW);
        totalDatas.add(mzXML_napedro_L120420_010_SW);

        for(float f : mzXML_napedro_L120224_010_SW){
            System.out.println(f);
        }
        return "charts/filereadspeed";

    }
}
