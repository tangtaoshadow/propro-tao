package com.westlake.air.propro.algorithm.compressor;

import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.util.List;

@Component("aircCompressor")
public class AircCompressor {

    public final Logger logger = LoggerFactory.getLogger(AircCompressor.class);

    @Autowired
    ConfigDAO configDAO;

    /**
     * @return
     */
    public ResultDO compress(BufferedOutputStream bos, List<AnalyseDataDO> dataList) {

        return new ResultDO(true);
    }

}
