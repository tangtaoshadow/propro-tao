package com.westlake.air.pecs.parser.model.mzxml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
public class Pattern {

    @XStreamAlias("spottingPattern")
    OntologyEntry spottingPattern;


    Orientation orientation;
}
