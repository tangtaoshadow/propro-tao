package com.westlake.air.propro.test.speed;

import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.compressor.AirdInfo;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.parser.AirdFileParser;
import com.westlake.air.propro.test.BaseTest;
import com.westlake.air.propro.utils.FileUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.TreeMap;

public class AirdReadSpeedTest extends BaseTest {

    @Autowired
    AirdFileParser airdFileParser;

    @Test
    public void airdReadSpeed() throws Exception {
        System.out.println("------ Aird Read Test ------");
        String filePath = "\\\\ProproNas\\ProproNAS\\data\\hye\\data\\HYE110_TTOF6600_32fix_lgillet_I160308_001.aird";
        String indexFilePath = "\\\\ProproNas\\ProproNAS\\data\\hye\\data\\HYE110_TTOF6600_32fix_lgillet_I160308_001.json";
        String jsonIndex = FileUtil.readFile(indexFilePath);
        AirdInfo airdInfo = JSONObject.parseObject(jsonIndex, AirdInfo.class);
        RandomAccessFile raf = new RandomAccessFile(new File(filePath), "r");
        long start = System.currentTimeMillis();
        int i=1;
        for(ScanIndexDO index : airdInfo.getSwathIndexList()){
            TreeMap<Float, MzIntensityPairs> result = airdFileParser.parseSwathBlockValues(raf, index, ByteOrder.LITTLE_ENDIAN);
            System.out.println("第"+i+"批数据,读取耗时:"+(System.currentTimeMillis() - start));
            start = System.currentTimeMillis();;
            i++;
        }

    }
}
