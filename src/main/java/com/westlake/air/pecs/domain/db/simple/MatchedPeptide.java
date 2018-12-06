package com.westlake.air.pecs.domain.db.simple;

import lombok.Data;

@Data
public class MatchedPeptide {

    String proteinName;

    String peptideRef;

    String analyseDataId;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof MatchedPeptide) {
            MatchedPeptide mp = (MatchedPeptide) obj;
            if (this.peptideRef == null || mp.getPeptideRef() == null) {
                return false;
            }

            return (this.peptideRef.equals(mp.getPeptideRef()));
        } else {
            return false;
        }

    }
}
