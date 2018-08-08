package com.westlake.air.pecs.domain.db.simple;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-06 10:48
 */
@Data
public class Peptide {

    String id;

    String transitionId;

    String libraryId;

    String libraryName;

    String proteinName;

    String peptideRef;

    Double rt;

    Boolean isDecoy;
}
