package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

/**
 * Chemical compound other than a peptide for which one or more transitions
 */
@Data
public class Compound {

    @XStreamImplicit(itemFieldName="cvParam")
    List<CvParam> cvParams;

    @XStreamImplicit(itemFieldName="userParam")
    List<UserParam> userParams;

    /**
     * List of retention time information entries
     */
    @XStreamAlias("RetentionTimeList")
    List<RetentionTime> retentionTimeList;
    /**
     * Identifier for the compound to be used for referencing within a document
     */
    @XStreamAsAttribute
    String id;
}
