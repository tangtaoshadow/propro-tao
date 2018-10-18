package com.westlake.air.pecs.domain.bean.compressor;

import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.db.simple.SimpleScanIndex;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class AirInfo {

    String compressionType = "zlib";
    String byteOrder = "network";
    String precision = "32";

    /**
     * Store the window rangs which have been adjusted with experiment overlap
     */
    List<WindowRang> windowRangs = new ArrayList<>();

    /**
     * the whole new scan index for new format file
     */
    List<ScanIndexDO> scanIndex = new ArrayList<>();

    /**
     * the swath window location(start and and) for new format file,
     * the key is the window rang which you can just focus on mzStart and mzEnd.
     * the value is the SimpleScanIndex, but you should only focus on the SimpleScanIndex.start and SimpleScanIndex.end properties.Other properties are not used
     */
    HashMap<WindowRang, SimpleScanIndex> mzRangScansMap = new HashMap<>();
}
