package com.westlake.air.pecs.domain.bean.analyse;

import com.westlake.air.pecs.domain.db.simple.TargetPeptide;
import lombok.Data;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-18 09:03
 */
@Data
public class LibraryCoordinate {

    String libraryId;

    Float rtExtractionWindow;

    List<TargetPeptide> ms1List;

    List<TargetPeptide> ms2List;
}
