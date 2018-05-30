package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

/**
 * Information about a single transition for a peptide or other compound
 */
@Data
@XStreamAlias("TargetList")
public class TargetList {

    @XStreamImplicit(itemFieldName="cvParam")
    List<CvParam> cvParams;

    @XStreamImplicit(itemFieldName="userParam")
    List<UserParam> userParams;

    /**
     * List of precursor m/z targets to include
     */
    @XStreamAlias("TargetIncludeList")
    List<Target> targetIncludeList;

    /**
     * List of precursor m/z targets to exclude
     */
    @XStreamAlias("TargetExcludeList")
    List<Target> targetExcludeList;
}
