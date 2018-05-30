package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

/**
 * Information about a prediction for a suitable transition using some software
 */
@Data
@XStreamAlias("Prediction")
public class Prediction {

    @XStreamImplicit(itemFieldName="cvParam")
    List<CvParam> cvParams;

    @XStreamImplicit(itemFieldName="userParam")
    List<UserParam> userParams;

    /**
     * Reference to a software package from which this prediction is derived
     * required
     */
    @XStreamAsAttribute
    String softwareRef;

    /**
     * Reference to a contact person that generated this prediction
     * optional
     */
    @XStreamAsAttribute
    String contactRef;
}
