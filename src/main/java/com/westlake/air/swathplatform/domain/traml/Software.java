package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

/**
 * Description of a software package used in the
 * generation of one or more transitions described in the document
 */
@Data
@XStreamAlias("Software")
public class Software {

    @XStreamImplicit(itemFieldName="cvParam")
    List<CvParam> cvParams;

    @XStreamImplicit(itemFieldName="userParam")
    List<UserParam> userParams;

    /**
     * Identifier for the software to be used for referencing within a document
     * required
     */
    @XStreamAsAttribute
    String id;

    /**
     * Version of the software program described
     * required
     */
    @XStreamAsAttribute
    String version;

}
