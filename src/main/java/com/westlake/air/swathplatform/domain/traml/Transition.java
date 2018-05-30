package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

/**
 * Information about a single transition for a peptide or other compound
 */
@Data
@XStreamAlias("Transition")
public class Transition {

    @XStreamAlias("Precursor")
    Precursor precursor;

    @XStreamImplicit(itemFieldName = "IntermediateProduct")
    List<IntermediateProduct> intermediateProductList;

    @XStreamAlias("Product")
    Product product;

    @XStreamAlias("RetentionTime")
    RetentionTime retentionTime;

    @XStreamAlias("Prediction")
    Prediction prediction;

    @XStreamImplicit(itemFieldName="cvParam")
    List<CvParam> cvParams;

    @XStreamImplicit(itemFieldName="userParam")
    List<UserParam> userParams;

    /**
     * Reference to a peptide which this transition is intended to identify
     * optional
     */
    @XStreamAsAttribute
    String peptideRef;

    /**
     * Reference to a compound for this transition
     * optional
     */
    @XStreamAsAttribute
    String compoundRef;

    /**
     * String label for this transition
     * required
     */
    @XStreamAsAttribute
    String id;

}
