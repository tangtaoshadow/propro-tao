package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

/**
 * Reference to a publication in which one or more transitions were published
 */
@Data
@XStreamAlias("Publication")
public class Publication {

    @XStreamImplicit(itemFieldName="cvParam")
    List<CvParam> cvParams;

    @XStreamImplicit(itemFieldName="userParam")
    List<UserParam> userParams;

    /**
     * Identifier for the publication to be used for referencing within a document
     * required
     */
    @XStreamAsAttribute
    String id;
}
