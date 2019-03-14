package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.algorithm.Airus;
import com.westlake.air.propro.algorithm.FragmentFactory;
import com.westlake.air.propro.async.LumsTask;
import com.westlake.air.propro.constants.PositionType;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.constants.TaskTemplate;
import com.westlake.air.propro.dao.AnalyseDataDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.compressor.AirdInfo;
import com.westlake.air.propro.domain.bean.scanindex.Position;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.parser.AirdFileParser;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.CompressUtil;
import com.westlake.air.propro.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.nio.ByteOrder;
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
    LumsTask lumsTask;
    @Autowired
    AirdFileParser airdFileParser;

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

    @RequestMapping("test3")
    @ResponseBody
    String test3(Model model, RedirectAttributes redirectAttributes) {
        ExperimentDO experimentDO = experimentService.getById("5b738f19e63cc81c44325169").getModel();

        LumsParams input = new LumsParams();
        input.setExperimentDO(experimentDO);
        input.setIRtLibraryId("5b67136d2ada5f15749a0140");
        input.setLibraryId("5b84bc9c58487f1060fa0c23");
        input.setCreator("陆妙善");
        input.setRtExtractWindow(RT_EXTRACT_WINDOW);
        input.setMzExtractWindow(MZ_EXTRACT_WINDOW);

        TaskDO taskDO = new TaskDO(TaskTemplate.TEST, "LMS-TEMP2");
        taskService.insert(taskDO);
        lumsTask.swath(input, taskDO);
        return "OK";
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
    String test8(Model model, RedirectAttributes redirectAttributes) throws IOException {

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setIsDecoy(false);
        query.setFdrEnd(0.01);
        query.setOverviewId("5c1c9a5acb15b6bb244d985e");
        query.setSortColumn("fdr");
        query.setPageSize(5000);
        ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);


        File file = new File("D://MatchedPeptide.txt");
        file.createNewFile();
        StringBuilder content = new StringBuilder();
        for (AnalyseDataDO data : resultDO.getModel()) {
            content.append(data.getPeptideRef()).append(",").append(data.getBestRt()).append("\r\n");
        }

        byte[] b = content.toString().getBytes();
        int l = b.length;
        OutputStream os = new FileOutputStream(file);
        os.write(b, 0, l);
        os.close();
        return "success";
    }
}
