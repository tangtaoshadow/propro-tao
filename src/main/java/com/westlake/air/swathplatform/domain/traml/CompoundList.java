package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

/**
 * List of compounds (including peptides) for which one or more transitions are intended to identify
 */
@Data
@XStreamAlias("CompoundList")
public class CompoundList {

    @XStreamImplicit(itemFieldName="cvParam")
    List<CvParam> cvParams;

    @XStreamImplicit(itemFieldName="userParam")
    List<UserParam> userParams;

    @XStreamImplicit(itemFieldName="Peptide")
    List<Peptide> peptideList;

    @XStreamImplicit(itemFieldName="Compound")
    List<Compound> compoundList;
}
