package com.westlake.air.swathplatform.parser.model.mzxml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;

@Data
public class Orientation {

    @XStreamAsAttribute
    protected String firstSpotID;

    @XStreamAsAttribute
    protected String secondSpotID;
}
