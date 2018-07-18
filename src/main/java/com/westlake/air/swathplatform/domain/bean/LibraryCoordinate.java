package com.westlake.air.swathplatform.domain.bean;

import lombok.Data;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-18 09:03
 */
@Data
public class LibraryCoordinate {

    String libraryId;

    Double rtExtractionWindow;

    List<TargetTransition> ms1List;

    List<TargetTransition> ms2List;
}
