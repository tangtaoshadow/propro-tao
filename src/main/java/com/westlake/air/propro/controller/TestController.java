package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.algorithm.Airus;
import com.westlake.air.propro.algorithm.FragmentFactory;
import com.westlake.air.propro.constants.PositionType;
import com.westlake.air.propro.constants.TaskTemplate;
import com.westlake.air.propro.dao.AnalyseDataDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.compressor.AirdInfo;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.parser.AirdFileParser;
import com.westlake.air.propro.parser.MzXMLParser;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.CompressUtil;
import com.westlake.air.propro.utils.FileUtil;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-07 13:30
 */
@Controller
@RequestMapping("test")
public class TestController extends BaseController {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    TaskService taskService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseDataDAO analyseDataDAO;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    Airus airus;
    @Autowired
    PeptideService peptideService;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    MzXMLParser mzXMLParser;

    public static float MZ_EXTRACT_WINDOW = 0.05f;
    public static float RT_EXTRACT_WINDOW = 1200f;
    public static float SIGMA = 6.25f;
    public static float SPACING = 0.01f;

    @RequestMapping("test")
    @ResponseBody
    String test(Model model, RedirectAttributes redirectAttributes) {

        try {
            ResultDO<ExperimentDO> expResult = experimentService.getById("5c75f7d9fc6f9e20a85e961a");
            ExperimentDO exp = expResult.getModel();
            RandomAccessFile raf = new RandomAccessFile(new File(exp.getAirdPath()), "r");
            HashMap<Float, ScanIndexDO> scanIndexMap = scanIndexService.getSwathIndexList("5c75f7d9fc6f9e20a85e961a");
            for (ScanIndexDO scanIndexDO : scanIndexMap.values()) {
                if (scanIndexDO.getPrecursorMzStart() == 400) {
                    try {
                        TreeMap map = airdFileParser.parseSwathBlockValues(raf, scanIndexDO, ByteOrder.LITTLE_ENDIAN);
                        System.out.println(scanIndexDO.getPrecursorMzStart() + ":" + map.size());
                    } catch (Exception e) {
                        logger.error("PrecursorMzStart: " + scanIndexDO.getPrecursorMzStart());
                        logger.error("Blocks: " + JSONArray.toJSONString(scanIndexDO.getBlocks()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //计算iRT
    @RequestMapping("test2")
    @ResponseBody
    String test2(Model model, RedirectAttributes redirectAttributes) {
        int[] mzArray = new int[21];
        mzArray[0] = 84960;
        mzArray[1] = 123015;
        mzArray[2] = 136919;
        mzArray[3] = 149015;
        mzArray[4] = 161057;
        mzArray[5] = 165694;
        mzArray[6] = 195918;
        mzArray[7] = 243131;
        mzArray[8] = 254097;
        mzArray[9] = 290156;
        mzArray[10] = 333144;
        mzArray[11] = 348147;
        mzArray[12] = 357035;
        mzArray[13] = 357232;
        mzArray[14] = 365206;
        mzArray[15] = 367164;
        mzArray[16] = 379980;
        mzArray[17] = 387205;
        mzArray[18] = 388134;
        mzArray[19] = 413090;
        mzArray[20] = 620326;
        int[] mzCompressedArray = CompressUtil.compressForSortedInt(mzArray);
        return "";

    }

    @RequestMapping("test6")
    @ResponseBody
    String test6(Model model, RedirectAttributes redirectAttributes) throws IOException {
        String jsonTest = "{\"start\": 482.14798,\"end\": 483.348,\"interval\": 1.20001221}";
        HashMap test = JSONObject.parseObject(jsonTest, HashMap.class);
        System.out.println(test.size());
        return null;
    }

    @RequestMapping("test8")
    @ResponseBody
    String test8(Model model, RedirectAttributes redirectAttributes) throws Exception {
        System.out.println("------ Aird Read Test ------");
        String filePath = "\\\\ProproNas\\ProproNAS\\data\\SGS\\aird\\napedro_L120420_010_SW.aird";
        String indexFilePath = "\\\\ProproNas\\ProproNAS\\data\\SGS\\aird\\napedro_L120420_010_SW.json";
        String jsonIndex = FileUtil.readFile(indexFilePath);
        AirdInfo airdInfo = JSONObject.parseObject(jsonIndex, AirdInfo.class);
        File jsonFile = new File(filePath);
        RandomAccessFile raf = new RandomAccessFile(jsonFile, "r");
        long start = System.currentTimeMillis();
        int i=1;
        System.out.println("napedro_L120224_010_SW.aird");
        for(ScanIndexDO index : airdInfo.getSwathIndexList()){
            TreeMap<Float, MzIntensityPairs> result = airdFileParser.parseSwathBlockValues(raf, index, ByteOrder.LITTLE_ENDIAN);
            System.out.println("第"+i+"批数据,读取耗时:"+(System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            i++;
        }

        FileUtil.close(raf);
        return null;
    }

    @RequestMapping("test9")
    @ResponseBody
    String test9(Model model, RedirectAttributes redirectAttributes) throws IOException {

        File file = new File("\\\\ProproNas\\ProproNAS\\data\\SGS\\mzxml\\napedro_L120224_010_SW.mzxml");
        List<ScanIndexDO> scanIndexes = mzXMLParser.index(file, 1);
        HashMap<Float, List<ScanIndexDO>> swathMap = new HashMap<>();
        for (ScanIndexDO index : scanIndexes) {
            if (index.getMsLevel().equals(2)) {
                List<ScanIndexDO> indexes = swathMap.get(index.getPrecursorMzStart());
                if (indexes == null) {
                    indexes = new ArrayList<>();
                    swathMap.put(index.getPrecursorMzStart(), indexes);
                }
                indexes.add(index);
            }
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");

            int i = 1;
            for (List<ScanIndexDO> indexes : swathMap.values()) {
                long start = System.currentTimeMillis();
                for (ScanIndexDO scanIndex : indexes) {
                    String value = mzXMLParser.parseValue(raf, scanIndex.getPosStart(PositionType.MZXML), scanIndex.getPosEnd(PositionType.MZXML));
                    Float[] values = mzXMLParser.getValues(new Base64().decode(value), 32, true, ByteOrder.BIG_ENDIAN);
                    List<Float> mzList = new ArrayList<>();
                    List<Float> intensityList = new ArrayList<>();
                    for (int peakIndex = 0; peakIndex < values.length - 1; peakIndex += 2) {
                        Float mz = values[peakIndex];
                        Float intensity = values[peakIndex + 1];
                        mzList.add(mz);
                        intensityList.add(intensity);
                    }
                }
                logger.info("第"+i+"批,耗时:"+(System.currentTimeMillis() - start));
                i++;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            FileUtil.close(raf);
        }

        return null;
    }


}
