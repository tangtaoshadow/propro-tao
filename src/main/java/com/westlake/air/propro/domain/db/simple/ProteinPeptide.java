package com.westlake.air.propro.domain.db.simple;

import lombok.Data;

@Data
public class ProteinPeptide {

    String proteinName;

    String peptideRef;

    Boolean isUnique;
}
