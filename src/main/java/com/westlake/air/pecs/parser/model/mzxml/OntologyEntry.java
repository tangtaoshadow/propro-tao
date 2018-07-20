package com.westlake.air.pecs.parser.model.mzxml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;

@Data
public class OntologyEntry {

    @XStreamAsAttribute
    String category;

    @XStreamAsAttribute
    String value;
}
