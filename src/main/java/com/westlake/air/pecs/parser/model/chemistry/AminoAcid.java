package com.westlake.air.pecs.parser.model.chemistry;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-11 23:53
 */
@Data
public class AminoAcid {

    String name;

    String shortName;

    String oneLetterCode;

    String formula;

    double monoIsotopicMass;

    double averageMass;
}
