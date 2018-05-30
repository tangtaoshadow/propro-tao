package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;

/**
 * Reference to a protein which this peptide is intended to identify
 */
@Data
public class ProteinRef {

    /**
     * Reference to a protein which this peptide is intended to identify
     * optional
     */
    @XStreamAsAttribute
    String ref;



}
